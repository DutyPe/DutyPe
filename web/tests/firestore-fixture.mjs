function state() {
  return globalThis.fixture;
}

function snapshot(reference, documents = state().documents) {
  if (state().deniedCollections?.includes(reference.path.split("/")[0])) throw new Error("Collection access denied");
  const data = structuredClone(documents[reference.path]);
  return {
    id: reference.id,
    ref: reference,
    exists: () => data !== undefined,
    data: () => structuredClone(data),
    get: (field) => data?.[field]
  };
}

function commit(operations) {
  const documents = structuredClone(state().documents);
  for (const operation of operations) {
    const { reference, data, merge, update } = operation;
    if (state().deniedCollections?.includes(reference.path.split("/")[0])) throw new Error("Collection access denied");
    if (state().failWritesTo?.includes(reference.path)) throw new Error("Synthetic write failure");
    if (update && !Object.hasOwn(documents, reference.path)) throw new Error("Document not found");
    const document = merge || update ? { ...documents[reference.path] } : {};
    for (const [field, value] of Object.entries(data)) {
      if (value?.operation === "increment") document[field] = (document[field] || 0) + value.amount;
      else if (value?.operation === "arrayUnion") document[field] = [...new Set([...(document[field] || []), ...value.values])];
      else if (value?.operation === "arrayRemove") document[field] = (document[field] || []).filter((item) => !value.values.includes(item));
      else document[field] = structuredClone(value);
    }
    documents[reference.path] = document;
  }
  state().documents = documents;
  state().writes.push(...operations.map(({ reference, data }) => ({ path: reference.path, data })));
  if (operations.some(({ reference }) => state().loseCommitResponseFor?.includes(reference.path))) {
    state().loseCommitResponseFor = [];
    throw new Error("Synthetic response lost after commit");
  }
  if (operations.some(({ reference }) => state().holdCommitResponseFor?.includes(reference.path))) {
    state().holdCommitResponseFor = [];
    return new Promise((resolve) => { (state().pendingCommitResponses ||= []).push({ resolve }); });
  }
}

export const collection = (database, name) => ({ path: name, collection: true });
export function doc(reference, ...segments) {
  const suffix = segments.length ? segments.join("/") : `generated-${++state().sequence}`;
  const path = reference.collection ? `${reference.path}/${suffix}` : suffix;
  return { path, id: path.split("/").at(-1) };
}
export const getDoc = async (reference) => {
  const result = snapshot(reference);
  if (reference.path.startsWith("users/")) {
    state().profileReads = (state().profileReads || 0) + 1;
    if (state().deferProfileReads) {
      return new Promise((resolve, reject) => {
        (state().pendingProfileReads ||= []).push({ path: reference.path, resolve: () => resolve(result), reject });
      });
    }
  }
  return result;
};
export const setDoc = async (reference, data, options) => commit([{ reference, data, merge: options?.merge }]);
export const updateDoc = async (reference, data) => {
  if (state().deferLoginMetadata && data.lastLoginAt) {
    return new Promise((resolve) => { state().finishLoginMetadata = () => { commit([{ reference, data, update: true }]); resolve(); }; });
  }
  return commit([{ reference, data, update: true }]);
};
export const deleteDoc = async (reference) => { delete state().documents[reference.path]; };
export function writeBatch() {
  const operations = [];
  return {
    set: (reference, data, options) => operations.push({ reference, data, merge: options?.merge }),
    update: (reference, data) => operations.push({ reference, data, update: true }),
    commit: async () => commit(operations)
  };
}
export const increment = (amount) => ({ operation: "increment", amount });
export const arrayUnion = (...values) => ({ operation: "arrayUnion", values });
export const arrayRemove = (...values) => ({ operation: "arrayRemove", values });
export const where = (field, operator, value) => ({ kind: "where", field, operator, value });
export const orderBy = (field, direction = "asc") => ({ kind: "order", field, direction });
export const limit = (maximum) => ({ kind: "limit", maximum });
export const query = (reference, ...constraints) => ({ ...reference, constraints });

export async function getDocs(reference) {
  let matches = Object.entries(state().documents).filter(([path]) =>
    path.startsWith(`${reference.path}/`) && path.split("/").length === reference.path.split("/").length + 1
  );
  for (const constraint of reference.constraints || []) {
    if (constraint.kind === "where") {
      matches = matches.filter(([, data]) => {
        const actual = data[constraint.field];
        if (constraint.operator === "==") return actual === constraint.value;
        if (constraint.operator === "in") return constraint.value.includes(actual);
        if (constraint.operator === "array-contains") return actual?.includes(constraint.value);
        throw new Error(`Unmodelled query operator: ${constraint.operator}`);
      });
    } else if (constraint.kind === "order") {
      matches.sort((left, right) => {
        const leftValue = left[1][constraint.field];
        const rightValue = right[1][constraint.field];
        return (leftValue < rightValue ? -1 : leftValue > rightValue ? 1 : 0) * (constraint.direction === "desc" ? -1 : 1);
      });
    } else if (constraint.kind === "limit") {
      matches = matches.slice(0, constraint.maximum);
    }
  }
  const docs = matches.map(([path]) => snapshot({ path, id: path.split("/").at(-1) }));
  return { docs, empty: docs.length === 0, size: docs.length };
}

export async function runTransaction(database, callback) {
  const documents = structuredClone(state().documents);
  const operations = [];
  const result = await callback({
    get: async (reference) => snapshot(reference, documents),
    set: (reference, data, options) => operations.push({ reference, data, merge: options?.merge }),
    update: (reference, data) => operations.push({ reference, data, update: true })
  });
  await commit(operations);
  state().transactionCompletions = (state().transactionCompletions || 0) + 1;
  return result;
}