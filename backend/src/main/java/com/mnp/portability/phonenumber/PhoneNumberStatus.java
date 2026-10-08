package com.mnp.portability.phonenumber;

/** Where a phone number stands in the portability system, from the viewpoint of the caller. */
public enum PhoneNumberStatus {

    /** Held by the operator whose range it was allocated from. */
    NOT_PORTED,

    /** Held by a different operator than the one whose range it was allocated from. */
    PORTED,

    /** A porting request is pending and the caller is its donor or recipient. */
    PORTING_PENDING
}