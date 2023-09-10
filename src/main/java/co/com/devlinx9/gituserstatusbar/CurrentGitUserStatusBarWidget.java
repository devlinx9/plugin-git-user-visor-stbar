package co.com.devlinx9.gituserstatusbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;

class CurrentGitUserStatusBarWidget extends EditorBasedWidget implements StatusBarWidget.MultipleTextValuesPresentation {

    private static final Logger LOGGER =
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private String text;
    private static final int INITIAL_DELAY = 15;
    private static final long DELAY = 30;


    CurrentGitUserStatusBarWidget(@NotNull Project project) {
        super(project);
    }

    @Override
    public @Nullable
    @NlsContexts.StatusBarText String getSelectedValue() {
        return text;
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
        return null;
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
        try {
            text = "git:";
            var project = this.getProject().getBasePath();
            if (project != null) {
                project = project.replace("file://", "");
            }

            if (!this.getProject().isDisposed()) {
                Editor fileEditorManager = FileEditorManager.getInstance(this.getProject()).getSelectedTextEditor();
                var mainProject = this.getProject();

                if (fileEditorManager != null) {
                    project = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                        @Override
                        public String compute() {
                            PsiFile psiFile = PsiDocumentManager.getInstance(mainProject).getPsiFile(fileEditorManager.getDocument());
                            return FileIndexFacade.getInstance(mainProject).getModuleForFile(psiFile.getVirtualFile()).getModuleFile().getParent().getCanonicalPath();
                        }
                    });
                }
            }


            LOGGER.info(project);
            text += executeCommand(String.format("git -C %s config --get user.name", project)).concat(":");
            text += executeCommand(String.format("git -C %s config --get user.email", project));

        } catch (IOException | InterruptedException e) {
            throw new GitUserVisorException(e);
        }

        if (myStatusBar != null) {
            myStatusBar.updateWidget(ID());
        }
    }

    private String executeCommand(String command) throws IOException, InterruptedException {
        String s;
        Process process;
        process = Runtime.getRuntime()
                .exec(command);
        String text2 = "";

        BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        while ((s = br.readLine()) != null) {
            LOGGER.info("line: {}".concat(s));
            text2 = s;

        }
        br.close();
        process.waitFor();
        int result = process.exitValue();
        if (result != 0) {
            LOGGER.info("git command result: ".concat(String.valueOf(result)));
        }
        process.destroy();
        return text2;
    }

}
