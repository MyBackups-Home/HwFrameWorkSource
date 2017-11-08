package com.android.server.security.trustcircle.tlv.command.login;

import com.android.server.security.trustcircle.tlv.core.TLVTree.TLVRootTree;

public class RET_LOGIN_CANCEL extends TLVRootTree {
    public static final int ID = -2147483631;

    public int getCmdID() {
        return ID;
    }

    public short getTreeTag() {
        return (short) 0;
    }
}
