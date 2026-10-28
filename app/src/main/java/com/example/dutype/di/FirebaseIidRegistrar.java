package com.example.dutype.di;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.components.Component;
import com.google.firebase.components.ComponentRegistrar;
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal;

import java.util.Collections;
import java.util.List;

/**
 * Enterprise Firebase Component Registrar Safeguard.
 * Provides FirebaseInstanceIdInternal to ComponentRuntime during cold start
 * so FirebaseInitProvider satisfies FunctionsMultiResourceComponent dependencies 100%.
 */
@Keep
public class FirebaseIidRegistrar implements ComponentRegistrar {

    private static final NoOpFirebaseInstanceIdInternal INSTANCE = new NoOpFirebaseInstanceIdInternal();

    @Keep
    public FirebaseIidRegistrar() {
    }

    @Keep
    public static final class NoOpFirebaseInstanceIdInternal implements FirebaseInstanceIdInternal {
        @Override
        @Nullable
        public String getId() {
            return "";
        }

        @Override
        @Nullable
        public String getToken() {
            return "";
        }

        @Override
        @NonNull
        public Task<String> getTokenTask() {
            return Tasks.forResult("");
        }

        @Override
        public void deleteToken(@NonNull String senderId, @NonNull String scope) {
        }

        @Override
        public void addNewTokenListener(@NonNull NewTokenListener listener) {
        }
    }

    @Override
    @NonNull
    public List<Component<?>> getComponents() {
        Component<FirebaseInstanceIdInternal> component = Component.builder(FirebaseInstanceIdInternal.class)
                .factory(new com.google.firebase.components.ComponentFactory<FirebaseInstanceIdInternal>() {
                    @Override
                    public FirebaseInstanceIdInternal create(com.google.firebase.components.ComponentContainer container) {
                        return INSTANCE;
                    }
                })
                .build();

        return Collections.singletonList(component);
    }
}
