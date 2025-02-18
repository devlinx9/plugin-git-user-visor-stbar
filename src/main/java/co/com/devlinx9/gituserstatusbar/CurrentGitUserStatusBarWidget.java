package co.com.devlinx9.gituserstatusbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;

class CurrentGitUserStatusBarWidget extends EditorBasedWidget implements StatusBarWidget.MultipleTextValuesPresentation {

    private static final Logger LOGGER =
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private String text;
    private static final int INITIAL_DELAY = 15;
    private static final long DELAY = 30;
    private static final int OUTPUT_LIMIT = 30;


    CurrentGitUserStatusBarWidget(@NotNull Project project) {
        super(project);
    }

    @Override
    public @Nullable
    @NlsContexts.StatusBarText String getSelectedValue() {
        if (text == null || text.length() <= OUTPUT_LIMIT) {
            return text;
        }
        return text.substring(0, OUTPUT_LIMIT) + "...";
    }

    @Override
    public @NonNls
    @NotNull String ID() {
        return "gitUserVisor";
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public @Nullable
    @NlsContexts.Tooltip String getTooltipText() {
        return text;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        super.install(statusBar);
        DumbService.getInstance(getProject()).runWhenSmart(this::updateGitUser);

        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
            LOGGER.info("Updating git user...");
            updateGitUser();
            LOGGER.info("finished update");
        }, INITIAL_DELAY, DELAY, SECONDS);
    }

    private void updateGitUser() {
        text = "git:";
        var project = this.getProject().getBasePath();
        if (project != null) {
            project = project.replace("file://", "");
        }

        if (!this.getProject().isDisposed()) {
            project = getPathFromEditorFile(project, this.getProject());
        }
        LOGGER.info(project);
        try {
            text += GitUtils.runCommand("git", "-C", project, "config", "--get", "user.name").concat(":");
            text += GitUtils.runCommand("git", "-C", project, "config", "--get", "user.email");
        } catch (GitUserVisorException e) {
            LOGGER.warning(e.getMessage());
        }


        if (myStatusBar != null) {
            myStatusBar.updateWidget(ID());
        }
    }

    private String getPathFromEditorFile(String project, Project mainProject) {
        Editor fileEditorManager = FileEditorManager.getInstance(this.getProject()).getSelectedTextEditor();

        if (fileEditorManager == null) {
            return project;
        }

        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            PsiFile psiFile = PsiDocumentManager.getInstance(mainProject)
                    .getPsiFile(fileEditorManager.getDocument());

            if (psiFile != null) {
                VirtualFile virtualFile = psiFile.getVirtualFile();
                if (virtualFile != null) {
                    Module module = ModuleUtilCore.findModuleForFile(virtualFile, mainProject);
                    if (module != null) {
                        VirtualFile moduleFile = ModuleRootManager.getInstance(module).getContentRoots()[0]; // Get first content root
                        if (moduleFile != null) {
                            return moduleFile.getCanonicalPath();
                        }
                    }
                }
            }
            return project;
        });

    }

}
