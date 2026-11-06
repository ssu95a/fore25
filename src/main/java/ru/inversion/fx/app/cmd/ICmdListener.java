package ru.inversion.fx.app.cmd;

/** */
public interface ICmdListener<B,LO> {
    /** */
    default void onRun( B cmd, LO objRun ) {}
    /** */
    default void onFinish( B cmd ) {}
}
