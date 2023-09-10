package co.com.devlinx9.gituserstatusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CurrentGitUserStatusBarWidgetFactory extends StatusBarEditorBasedWidgetFactory {
    @Override
    public @NonNls @NotNull String getId() {
        return "GitUserVisor";
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return "Git user visor";
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new CurrentGitUserStatusBarWidget(project);
    }
}
