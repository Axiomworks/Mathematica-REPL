import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.IconLoader;

public class MathREPLStartAction extends MathREPLBaseAction {


    public static final String REPL_TITLE = "Mathematica REPL";
    //public static final String REPL_MAIN_CLASS = "javarepl.Repl";

    //public static final String EXECUTE_ACTION_IMMEDIATELY_ID = "JavaREPL.Console.Execute.Immediately";

    public MathREPLStartAction(){
       super();
    }
    @Override
    public void update(AnActionEvent e)
    {
        final Module m = getModule(e);
        final Presentation presentation = e.getPresentation();
        if (m == null) {
            presentation.setEnabled(false);
            return;
        }
        presentation.setEnabled(true);
        super.update(e);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        //final Module module = getModule(event);
        //assert module != null : "Module is null";

        //final String path = ModuleRootManager.getInstance(module).getContentRoots()[0].getPath();

        //try {
           // JavaREPLConsoleRunner.run(module, path);
        //} catch (CantRunException e) {
           // ExecutionHelper.showErrors(module.getProject(), Arrays.<Exception>asList(e), REPL_TITLE, null);
        //}
        new MathREPLConsoleImpl().run(new String[] {"",""});
    }

    static Module getModule(AnActionEvent e) {
        Module module = e.getData(DataKeys.MODULE);
        if (module == null) {
            final Project project = e.getData(DataKeys.PROJECT);
            if (project == null) return null;
            final Module[] modules = ModuleManager.getInstance(project).getModules();
            if (modules.length == 1) {
                module = modules[0];
            } else {
                if (modules.length > 0) {
                    module = modules[0];
                }
            }
        }
        return module;
    }
}