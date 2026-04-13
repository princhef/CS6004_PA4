import java.util.Map;

import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

public class MainClass {

    public static CallGraph cg; // shared

    public static void main(String[] args) {
        String classPath = ".";

        String[] sootArgs = {
            "-cp", classPath, "-pp", 
            "-f", "j",
            "-keep-line-number",
            "-no-bodies-for-excluded",
            "-p", "jb", "use-original-names",
            "-keep-bytecode-offset", "-keep-offset",
            "-main-class", "Test", "Test", "A","-w","-p","cg.spark","enabled:true"
        };

        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("cg.spark", "on");
        
        // capture CG after it's built
        /*PackManager.v().getPack("wjtp").add(
            new Transform("wjtp.cgGrabber", new SceneTransformer() {
                @Override
                protected void internalTransform(String phaseName, Map options) {
                    //cg = Scene.v().getCallGraph();
                }
            })
        );*/

        //PackManager.v().getPack("jtp").add(new Transform("jtp.sample", new AddTransform()));
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.sample", new AddTransform()));
        
        soot.Main.main(sootArgs);
    }
}
