// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.JWTParser;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

@Accessors(fluent = true)
@Getter
@EqualsAndHashCode
@Builder
final class AuthenticationResult implements Serializable, IAuthenticationResult {
    private static final long serialVersionUID = 1L;

    private final String accessToken;

    @Getter(value = AccessLevel.PACKAGE)
    private final long expiresOn;

    @Getter(value = AccessLevel.PACKAGE)
    private final long extExpiresOn;

    private final String refreshToken;

    @Getter(value = AccessLevel.PACKAGE)
    private final String familyId;

    private final String idToken;

    @Getter(value = AccessLevel.PACKAGE, lazy = true)
    private final IdToken idTokenObject = getIdTokenObj();

    private IdToken getIdTokenObj() {
        if (StringHelper.isBlank(idToken)) {
            return null;
        }
        try {
            String idTokenJson = JWTParser.parse(idToken).getParsedParts()[1].decodeToString();
            return JsonHelper.convertJsonToObject(idTokenJson, IdToken.class);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Getter(value = AccessLevel.PACKAGE)
    private final AccountCacheEntity accountCacheEntity;

    @Getter(lazy = true)
    private final IAccount account = getAccount();

    private IAccount getAccount() {
        if (accountCacheEntity == null) {
            return null;
        }
        return accountCacheEntity.toAccount();
    }

    private String environment;

    @Getter(lazy = true)
    private final Date expiresOnDate = new Date(expiresOn * 1000);

    private final String scopes;
}
