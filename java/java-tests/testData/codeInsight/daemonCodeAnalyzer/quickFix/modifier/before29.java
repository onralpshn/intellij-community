// "Make 'c class initializer' not static" "true"
import java.io.*;

class c {
    class inner {
        class ininner {}
    }

    static {
        <caret>new inner();
    }
}
