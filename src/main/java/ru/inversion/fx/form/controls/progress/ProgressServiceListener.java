package ru.inversion.fx.form.controls.progress;

@FunctionalInterface
interface ProgressServiceListener {
    public void invoke(AbstractProgressTask progressTask);
}
