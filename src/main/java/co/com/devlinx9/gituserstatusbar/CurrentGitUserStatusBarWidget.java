package co.com.devlinx9.gituserstatusbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.FileIndexFacade;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
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
                project = getPathFromEditorFile(project, this.getProject());
            }
            LOGGER.info(project);
            text += executeCommand(String.format("git -C %s config --get user.name", project)).concat(":");
            text += executeCommand(String.format("git -C %s config --get user.email", project));

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitUserVisorException(e);
        }

        if (myStatusBar != null) {
            myStatusBar.updateWidget(ID());
        }
    }

    private String executeCommand(String command) throws IOException, InterruptedException {
        String s;
        StringBuilder output = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", command); // Use "/bin/sh -c" for shell commands
        processBuilder.redirectErrorStream(true); // Redirect error stream to standard output

        Process process = processBuilder.start();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            while ((s = br.readLine()) != null) {
                LOGGER.log(Level.INFO, "line: {0}", s);
                output.append(s).append("\n"); // Append the output to the StringBuilder
            }
        }

        int result = process.waitFor(); // Wait for the process to complete
        if (result != 0) {
            LOGGER.log(Level.INFO, "command result: {0}", result);
        }

        process.destroy();
        return output.toString().trim(); // Return the full output as a single string
    }

    private String getPathFromEditorFile(String project, Project mainProject) {
        Editor fileEditorManager = FileEditorManager.getInstance(this.getProject()).getSelectedTextEditor();

        if (fileEditorManager != null) {
            return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
                PsiFile psiFile = PsiDocumentManager.getInstance(mainProject).getPsiFile(fileEditorManager.getDocument());
                if (psiFile != null) {
                    Module moduleForFile = FileIndexFacade.getInstance(mainProject).getModuleForFile(psiFile.getVirtualFile());
                    if (moduleForFile != null) {
                        VirtualFile virtualFile = ProjectUtil.guessModuleDir(moduleForFile);
                        if (virtualFile != null) {
                            return virtualFile.getCanonicalPath();
                        }
                    }
                }
                return project;
            });
        }
        return project;
    }

}
