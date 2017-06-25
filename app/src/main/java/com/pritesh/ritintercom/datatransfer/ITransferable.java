package com.pritesh.ritintercom.datatransfer;

import java.io.Serializable;


public interface ITransferable extends Serializable {

    int getRequestCode();

    String getRequestType();

    String getData();
}
