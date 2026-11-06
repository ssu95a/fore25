package ru.inversion.fx.form.valid;

/**
 *
 * @author ssu @
 */
public interface Validator<T> {

    public class Result {

        private Object errorCode;
        private String description;
        private Object userData;
        private Exception exception;

        public Result(Object errorCode, String description, Object userData) {
            this.errorCode = errorCode;
            this.description = description;
            this.userData = userData;
        }

        public Result(Exception exception) {
            this.exception = exception;
        }

        /**
         *
         */
        public Result(Object errorCode, String description) {
            this(errorCode, description, null);
        }

        /**
         *
         */
        public Result(Object errorCode) {
            this(errorCode, null);
        }

        /**
         *
         */
        public Object getErrorCode() {
            return errorCode;
        }

        /**
         *
         */
        public String getDescription() {
            return description;
        }

        /**
         *
         */
        public Object getUserData() {
            return userData;
        }

        public Exception getException() {
            return exception;
        }

    }

    /**
     *
     */
    Result validate(T value);
}
