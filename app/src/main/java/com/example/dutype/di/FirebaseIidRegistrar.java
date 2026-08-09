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

    @Override
    @NonNull
    public List<Component<?>> getComponents() {
        FirebaseInstanceIdInternal provider = new FirebaseInstanceIdInternal() {
            @Override
            @Nullable
            public String getId() {
                return null;
            }

            @Override
            @Nullable
            public String getToken() {
                return null;
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
        };

        Component<FirebaseInstanceIdInternal> component = Component.builder(FirebaseInstanceIdInternal.class)
                .factory(container -> provider)
                .build();

        return Collections.singletonList(component);
    }
}
