// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jsample;

/**
 * @author Azure Active Directory Contributor
 *
 */
public abstract class DirectoryObject {
	
	public DirectoryObject() {
		super();
	}
	
	/**
	 * 
	 * @return
	 */
	public abstract String getObjectId();
	
	/**
	 * @param objectId
	 */
	public abstract void setObjectId(String objectId);

	/**
	 * 
	 * @return
	 */
	public abstract String getObjectType();

	/**
	 * 
	 * @param objectType
	 */
	public abstract void setObjectType(String objectType);
	
	/**
	 * 
	 * @return
	 */
	public abstract String getDisplayName();

	/**
	 * 
	 * @param displayName
	 */
	public abstract void setDisplayName(String displayName);

}
