package nl.aurorion.blockregen.region;

import nl.aurorion.blockregen.util.BlockPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface Region {
    @NotNull String getName();

    @Nullable Boolean getDisableOtherBreak();

    void setDisableOtherBreak(@Nullable Boolean value);

    int getPriority();

    void setPriority(int priority);

    void setAll(boolean value);

    boolean isAll();

    boolean hasPreset(@NotNull String preset);

    void addPreset(@NotNull String preset);

    void removePreset(@NotNull String preset);

    void clearPresets();

    @NotNull Collection<String> getPresets();

    boolean contains(@NotNull BlockPosition position);
}
