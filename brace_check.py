import sys; content = sys.stdin.read(); stack = []; lines = content.split('\n')
def get_line(idx):
    curr = 0
    for l_no, l in enumerate(lines):
        if curr + len(l) + 1 > idx: return l_no + 1
        curr += len(l) + 1
    return -1
for i, c in enumerate(content):
    if c == '{': stack.append(('{', i))
    elif c == '}':
        if not stack: print(f'Extra }} at line {get_line(i)}')
        else: stack.pop()
if stack:
    print('Unclosed {')
    for brace, idx in stack:
        print(f'Line {get_line(idx)}')

