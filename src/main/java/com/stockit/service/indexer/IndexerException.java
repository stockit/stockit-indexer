package com.stockit.service.indexer;

/**
 * Created by dmcquill on 3/23/15.
 */
public class IndexerException extends RuntimeException {

    public IndexerException() {
        super();
    }

    public IndexerException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public IndexerException(String msg) {
        super(msg);
    }

    public IndexerException(Throwable cause) {
        super(cause);
    }
}
