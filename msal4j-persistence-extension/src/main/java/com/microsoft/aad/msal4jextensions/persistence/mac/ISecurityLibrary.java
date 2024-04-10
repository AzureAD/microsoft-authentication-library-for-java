// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Interface which represents API for accessing KeyChain on Mac.
 * It is used for storing and retrieving passwords and other secrets on Mac.
 * https://developer.apple.com/documentation/security/keychain_services/keychain_items
 */
public interface ISecurityLibrary extends Library {

    int ERR_SEC_SUCCESS = 0;
    int ERR_SEC_ITEM_NOT_FOUND = -25300;

    ISecurityLibrary library = Native.load("Security", ISecurityLibrary.class);

    /**
     * Adds a new generic password to a keychain.
     *
     * @param keychain          A reference to the keychain in which to store a generic password.
     *                          Pass NULL to specify the default keychain.
     * @param serviceNameLength The length of the serviceName character string.
     * @param serviceName       A UTF-8 encoded character string representing the service name.
     * @param accountNameLength The length of the accountName character string.
     * @param accountName       A UTF-8 encoded character string representing the account name.
     * @param passwordLength    The length of the passwordData buffer.
     * @param passwordData      A pointer to a buffer containing the password data to be stored in the keychain.
     *                          Before calling this function, allocate enough memory for the buffer to hold the data you want to store.
     * @param itemRef           On return, a pointer to a reference to the new keychain item.
     *                          Pass NULL if you don’t want to obtain this object. You must allocate the memory for this pointer.
     *                          You must call the CFRelease function to release this object when you are finished using it.
     * @return A result code.
     * https://developer.apple.com/documentation/security/1542001-security_framework_result_codes
     */
    int SecKeychainAddGenericPassword(
            Pointer keychain,
            int serviceNameLength,
            byte[] serviceName,
            int accountNameLength,
            byte[] accountName,
            int passwordLength,
            byte[] passwordData,
            Pointer itemRef
    );

    /**
     * Updates an existing keychain item after changing its attributes and/or data.
     *
     * @param itemRef  A reference to the keychain item to modify.
     * @param attrList A pointer to the list of attributes to set and their new values.
     *                 Pass NULL if you have no need to modify attributes.
     * @param length   The length of the buffer pointed to by the data parameter.
     *                 Pass 0 if you pass NULL in the data parameter.
     * @param data     A pointer to a buffer containing the data to store. Pass NULL if you do not need to modify the data.
     * @return A result code.
     * https://developer.apple.com/documentation/security/1542001-security_framework_result_codes
     */
    int SecKeychainItemModifyContent(
            Pointer itemRef,
            Pointer attrList,
            int length,
            byte[] data
    );

    /**
     * Finds the first generic password based on the attributes passed.
     *
     * @param keychainOrArray   A reference to an array of keychains to search,
     *                          a single keychain, or NULL to search the user’s default keychain search list.
     * @param serviceNameLength The length of the serviceName character string.
     * @param serviceName       A UTF-8 encoded character string representing the service name.
     * @param accountNameLength The length of the accountName character string.
     * @param accountName       A UTF-8 encoded character string representing the account name.
     * @param passwordLength    On return, the length of the buffer pointed to by passwordData.
     * @param passwordData      On return, a pointer to a buffer that holds the password data.
     *                          Pass NULL if you want to obtain the item object but not the password data.
     *                          In this case, you must also pass NULL in the passwordLength parameter.
     *                          You should use the SecKeychainItemFreeContent function
     *                          to free the memory pointed to by this parameter.
     * @param itemRef           On return, a pointer to the item object of the generic password.
     *                          You are responsible for releasing your reference to this object.
     *                          Pass NULL if you don’t want to obtain this object.
     * @return A result code
     * https://developer.apple.com/documentation/security/1542001-security_framework_result_codes
     */
    int SecKeychainFindGenericPassword(
            Pointer keychainOrArray,
            int serviceNameLength,
            byte[] serviceName,
            int accountNameLength,
            byte[] accountName,
            int[] passwordLength,
            Pointer[] passwordData,
            Pointer[] itemRef
    );

    /**
     * Deletes a keychain item from the default keychain’s permanent data store.
     *
     * @param itemRef A keychain item object of the item to delete.
     *                You must call the CFRelease function to release this object when you are finished using it.
     * @return A result code.
     * https://developer.apple.com/documentation/security/1542001-security_framework_result_codes
     */
    int SecKeychainItemDelete(
            Pointer itemRef
    );

    /**
     * Returns a string explaining the meaning of a security result code.
     *
     * @param status   A result code of type OSStatus returned by a security function.
     *                 https://developer.apple.com/documentation/security/1542001-security_framework_result_codes
     * @param reserved Reserved for future use. Pass NULL for this parameter.
     * @return A human-readable string describing the result, or NULL if no string is available for the specified result code.
     * Call the CFRelease function to release this object when you are finished using it.
     */
    Pointer SecCopyErrorMessageString(
            int status,
            Pointer reserved);

    /**
     * Returns the number (in terms of UTF-16 code pairs) of Unicode characters in a string.
     *
     * @param theString The string to examine.
     * @return The number (in terms of UTF-16 code pairs) of characters stored in theString.
     */
    int CFStringGetLength(
            Pointer theString
    );

    /**
     * Returns the Unicode character at a specified location in a string.
     *
     * @param theString The string from which the Unicode character is obtained.
     * @param idx       The position of the Unicode character in the CFString.
     * @return A Unicode character.
     */
    char CFStringGetCharacterAtIndex(
            Pointer theString,
            long idx
    );

    /**
     * Releases a Core Foundation object.
     *
     * @param cf CFType object to release. This value must not be NULL.
     */
    void CFRelease(
            Pointer cf
    );

    /**
     * Releases the memory used by the keychain attribute list and the keychain data.
     *
     * @param attrList A pointer to the attribute list to release. Pass NULL if there is no attribute list to release.
     * @param data     A pointer to the data buffer to release. Pass NULL if there is no data to release.
     * @return A result code.
     * https://developer.apple.com/documentation/security/1542001-security_framework_result_codes
     */
    int SecKeychainItemFreeContent(
            Pointer[] attrList,
            Pointer data);
}
