//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package labapi;

public class LabUserNotFoundException extends IllegalArgumentException {

    private UserQuery parameters;

    public LabUserNotFoundException(UserQuery parameters, String message){
        super(message);

        this.parameters = parameters;
    }

    public UserQuery getParameters() {
        return parameters;
    }
}
