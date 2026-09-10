// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Options controlling an mTLS Proof-of-Possession token request.
 */
public final class MtlsPopOptions {

    private final MtlsBindingStrength minimumBindingStrength;

    private MtlsPopOptions(Builder builder) {
        this.minimumBindingStrength = builder.minimumBindingStrength;
    }

    public MtlsBindingStrength minimumBindingStrength() {
        return minimumBindingStrength;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private MtlsBindingStrength minimumBindingStrength =
                MtlsBindingStrength.NONE;

        private Builder() {
        }

        /**
         * Sets the minimum binding strength required for acquisition to succeed.
         */
        public Builder minimumBindingStrength(
                MtlsBindingStrength minimumBindingStrength) {
            if (minimumBindingStrength == null) {
                throw new NullPointerException("minimumBindingStrength");
            }
            this.minimumBindingStrength = minimumBindingStrength;
            return this;
        }

        public MtlsPopOptions build() {
            return new MtlsPopOptions(this);
        }
    }
}
