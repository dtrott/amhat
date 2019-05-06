package ham.arhat.config.loader;

public class KeyPairLoadFailedException extends Exception {
    public KeyPairLoadFailedException() {
    }

    public KeyPairLoadFailedException(String message) {
        super(message);
    }

    public KeyPairLoadFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyPairLoadFailedException(Throwable cause) {
        super(cause);
    }
}
