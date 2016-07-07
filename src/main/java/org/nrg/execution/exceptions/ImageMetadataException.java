package org.nrg.execution.exceptions;

@SuppressWarnings("unused")
public class ImageMetadataException extends RuntimeException {
    public ImageMetadataException(Throwable cause) {
        super(cause);
    }

    public ImageMetadataException(String message) {
        super(message);
    }

    public ImageMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
