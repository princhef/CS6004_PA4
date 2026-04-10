import soot.PackManager;
import soot.Transform;

public class MainClass {
    public static void main(String[] args) {
        String classPath = ".";

        String[] sootArgs = {
            "-cp", classPath, "-pp", 
            "-f", "c",
            "-keep-line-number",
            "-no-bodies-for-excluded",
            "-p", "jb", "use-original-names",
            "-keep-bytecode-offset", "-keep-offset",
            "-main-class", "Test", "Test", "A"
        };

        PackManager.v().getPack("jtp").add(new Transform("jtp.sample", new AddTransform()));
        soot.Main.main(sootArgs);
    }
}
