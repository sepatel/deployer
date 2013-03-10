/**
 * Date: 3/9/13 9:52 PM
 *
 * @author <a href='mailto:sejal@sejal.org">Sejal Patel</a>
 */
public class DotException extends RuntimeException {
    public DotException() {
        super();
    }

    public DotException(String message) {
        super(message);
    }

    public DotException(String message, Throwable cause) {
        super(message, cause);
    }

    public DotException(Throwable cause) {
        super(cause);
    }
}
