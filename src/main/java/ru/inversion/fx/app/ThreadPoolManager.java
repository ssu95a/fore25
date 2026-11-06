package ru.inversion.fx.app;

import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class ThreadPoolManager {

    private static final int THREAD_P_S_SIZE = 12;
//    private static final int THREAD_P_SIZE = 2 * Runtime.getRuntime().availableProcessors() + 1;


    private static final int FIXED_SIZE_DEFAULT = 16;   //
    private static final int FIXED_QUEUE_CAPACITY = 2000;

    private static final int CACHED_MAX_THREADS = 64;

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolManager.class);
    private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();

    // Пул для задач с повтором исполнения или с отложенным исполнением
    private final ScheduledThreadPoolExecutor scheduledThreadPool;

    // Основной пул для выполнения любых задач
    private final ThreadPoolExecutor fixedThreadPool;

    // Пул для небольших задач (с коротким временем исполнения)
    private final ThreadPoolExecutor cacheTreadPool;

    private volatile boolean shutdown;

    /** */
    private ThreadPoolManager() {

        scheduledThreadPool = new ScheduledThreadPoolExecutor( THREAD_P_S_SIZE, new PriorityThreadFactory("fore-Scheduled-Th-Pool", Thread.NORM_PRIORITY) );
        scheduledThreadPool.setRemoveOnCancelPolicy(true);

        int fixedSize = FIXED_SIZE_DEFAULT; // лучше: читать из конфига

        fixedThreadPool = new ThreadPoolExecutor (
                fixedSize,
                fixedSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(FIXED_QUEUE_CAPACITY),
                new PriorityThreadFactory("fore-Th-Pool", Thread.NORM_PRIORITY),
                new RejectedExecutionHandler() {
                    @Override public void rejectedExecution( Runnable r, ThreadPoolExecutor e) {

                        if( e.isShutdown() )
                            throw new RejectedExecutionException("Executor is shutdown");

                        if (javafx.application.Platform.isFxApplicationThread())
                            throw new RejectedExecutionException("Rejected on FX thread");

                        r.run();
                    }
                }
        );

        cacheTreadPool = new ThreadPoolExecutor(
            0,
            CACHED_MAX_THREADS,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new DaemonThreadFactory("fore-Cached-Th-Pool"),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /** */
    public static ThreadPoolManager getInstance() {
        return INSTANCE;
    }

    /** */
    public ExecutorService fixedExecutor() { return fixedThreadPool; }

    /** */
    public ExecutorService shortExecutor() { return cacheTreadPool; }

    /** */
    public ExecutorService scheduledExecutor() {
        return scheduledThreadPool;
    }

    /** Выполнить коллекцию задач и получать результаты по мере готовности */
    public <T> Future<?> executeAllAsCompleted(
          Collection<? extends Callable<T>> tasks,
            Consumer<? super T> onSuccess,
            Consumer<? super Throwable> onError,
            Consumer<? super Pair<Integer, Integer>> onProgress,  // done,total
            Consumer<? super Throwable> onComplete                // null=ok, InterruptedException=cancel, other=crash
    )
    {
        if( tasks == null || tasks.isEmpty() )
        {
            if (onProgress != null) onProgress.accept(new Pair<>(0, 0));
            if (onComplete != null) onComplete.accept(null);
            return CompletableFuture.completedFuture (null);
        }

        // возвращаем как Future, чтобы можно было cancel(true)
        return cacheTreadPool.submit( new RunnableWrapper(() -> {

            final int total = tasks.size();
            int done = 0;

            Throwable completionCause = null;

            // CompletionService привязан к fixedThreadPool (рабочим потокам)
            final ExecutorCompletionService<T> ecs = new ExecutorCompletionService<>(fixedThreadPool);

            // Храним все submitted futures, чтобы можно было отменить при cancel
            final List<Future<T>> submitted = new ArrayList<>(total);

            try {

                // submit всех задач
                for( Callable<T> c : tasks )
                {
                    // Если orchestration уже прервали до submit
                    if( Thread.currentThread().isInterrupted() )
                        throw new InterruptedException("Orchestration interrupted before submit()");

                    submitted.add(ecs.submit(c));
                }

                // забираем результаты по мере готовности
                while( done < submitted.size())
                {
                    // take() interruptible — это и есть cancel
                    Future<T> f = ecs.take();

                    try {

                        T res = f.get();

                        if( onSuccess != null )
                            onSuccess.accept(res);

                    } catch (CancellationException ce) {
                        // конкретная задача могла быть отменена (например при cancel батча)
                        if (onError != null)
                            onError.accept(ce);

                    } catch (ExecutionException ee) {
                        Throwable cause = (ee.getCause() != null) ? ee.getCause() : ee;
                        if (onError != null)
                            onError.accept(cause);

                    } finally
                    {
                        done++;

                        if( onProgress != null )
                            onProgress.accept(new Pair<>(done, total));
                    }
                }

            } catch(InterruptedException ie) {
                // cancel/stop — НЕ ошибка задачи, а состояние завершения батча
                Thread.currentThread().interrupt();
                completionCause = ie;

            } catch (RejectedExecutionException rex) {
                completionCause = rex;
            } catch (Throwable th) {
                completionCause = th;

            } finally {
                if( completionCause instanceof InterruptedException )
                {
                    for( Future<T> f : submitted ) {
                        if (!f.isDone()) {
                            f.cancel(true); // interrupt worker thread (best-effort)
                        }
                    }
                }
                if (onComplete != null) {
                    onComplete.accept(completionCause); // null=OK
                }
            }
        }));
    }


    /**
     * Запустить задачу после определенной задержки.
     *
     * @param task  задача для выполнения
     * @param delay задержка в миллисекундах
     */
    public ScheduledFuture<?> schedule( Runnable task, long delay ) {
        return schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Запустить задачу после определенной задержки.
     *
     * @param task  задача для выполения
     * @param delay задержка
     * @param unit  time unit для delay
     */
    public ScheduledFuture<?> schedule( Runnable task, long delay, TimeUnit unit ) {
        try {
            return scheduledThreadPool.schedule(new RunnableWrapper(task), delay, unit);
        } catch (RejectedExecutionException e) {
            log.error("Can't execute task", e);
            return null;
        }
    }

    /**
     * Запуск задачи с фиксированным промежутком выполнения.
     *
     * @param task         задача для выполнения
     * @param initialDelay время ожидания перед первым запуском
     * @param period       периодичность запуска
     * @param unit         time unit для initialDelay и period
     * @return возвращает ScheduledFuture который представляет задачу в процессе выполнения.
     * null если задача не может быть исполнена
     */
    public ScheduledFuture<?> scheduleAtFixedRate( Runnable task, long initialDelay, long period, TimeUnit unit ) {
        try {
            return scheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(task), initialDelay, period, unit);
        } catch (RejectedExecutionException e) {
            log.error("Can't execute task", e);
            return null;
        }
    }

    /**
     * Запуск задачи с фиксированным промежутком выполнения
     * Отличия данного метода от <code>scheduleAtFixedRate</code>
     * в том, что отсчет повторного выполнения начинается от окончания предыдущего
     *
     * @param task         задача для выполнения
     * @param initialDelay время ожидания перед первым запуском
     * @param period       периодичность запуска
     * @param unit         time unit для initialDelay и period
     *
     * @return возвращает ScheduledFuture который, представляет задачу в процессе выполнения,
     * <code>null</code> если задача не может быть исполнена
     */
    public ScheduledFuture<?> scheduleWithFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        try {
            return scheduledThreadPool.scheduleWithFixedDelay(new RunnableWrapper(task), initialDelay, period, unit);
        } catch (RejectedExecutionException e) {
            log.error("Can't execute task", e);
            return null;
        }
    }

    /**
     * Выполнение задачи в отдельном потоке
     * @param task задача для выполнения
     */
    public void executeTask(Runnable task) {
        fixedThreadPool.execute(new RunnableWrapper(task));
    }

    /**
     * Выполнение задачи в отдельном потоке с результатом выполнения
     *
     * @param task задача для выполнения
     * @param <T>  тип результа выполняемой задачи
     * @return возвращает Future который представляет задачу в процессе выполнения.
     * null если задача не может быть исполнена
     */
    public <T> Future<T> executeTask(Callable<T> task) {
        return fixedThreadPool.submit(task);
    }

    /**
     * Выполнение задачи в отдельном потоке с результатом выполнения.
     * В данном случае, результатом будет то, что было передано
     * вторым аргументом - <code>result</code>
     *
     * @param task   задача для выполнения
     * @param result результат
     * @param <T>    тип результата
     * @return возвращает Future который представляет задачу в процессе выполнения.
     * null если задача не может быть исполнена
     */
    public <T> Future<? super T> executeTask( Runnable task, T result ) {
        return fixedThreadPool.submit( new RunnableWrapper(task), result);
    }

    /**
     * <p>
     * Выполнение списка задач в отдельных потоках с результатом выполнения
     * </p>
     * <p>
     * <b>Метод заблокирует текущий поток</b>, поэтому если вы хотите выполнить несколько задач,
     * но при этом не мешать основному потоку - вызывайте данный метод в отдельном потоке
     * <p>К примеру вот так:</p>
     * <pre>
     *     ThreadPoolManager.getInstance().executeTask(() -> {
     *          final List<Future<String>> futures = ThreadPoolManager.getInstance().executeAllTask(callables);
     *     });
     * </pre>
     *
     * @param callables список задач
     * @param <T>       тип результа выполняемых задач
     * @return возвращает список Future в котором каждый из них представляет задачу в процессе выполнения.
     * null если задачи были прерваны
     */
    public <T> List<Future<T>> executeAllTask( Collection<Callable<T>> callables ) {

        try {

            if( callables == null || callables.isEmpty() )
                return Collections.emptyList();

            return fixedThreadPool.invokeAll(callables);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();

            log.error("Executing tasks was interrupted", e);
            return Collections.emptyList();
        }
    }

    /**
     * Выполнение списка задач в отдельных потоках с результатом выполнения.
     * Можно задать timeout выполнения, он будет применен для каждой задачи в списке.
     *
     * @param callables список задач
     * @param timeout   максимальное время ожидания
     * @param unit      time unit для timeout
     * @param <T>       тип результата выполняемых задач
     * @return возвращает список Future в котором каждый из них представляет задачу в процессе выполнения.
     * null если задачи были прерваны
     */
    public <T> List<Future<T>> executeAllTask(Collection<Callable<T>> callables, long timeout, TimeUnit unit) {
        try {

            if( callables == null || callables.isEmpty() )
                return Collections.emptyList();

            return fixedThreadPool.invokeAll( callables, timeout, unit );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            log.error("Executing tasks was interrupted", e);
            return Collections.emptyList();
        }
    }

    /**
     * Выполнение задачи в отдельном потоке и возвращение результата
     * <p>
     * <b>Данный метод является блокирующим, так как ожидает выполнения задачи,
     * если будете выполнять его в JavaFX потоке, то программа "повиснет" </b>
     *
     * @param task задача для выполнения
     * @param <T>  тип результата
     * @return результат выполненной задачи
     */
    public <T> T executeAndReturn(Callable<T> task) {
        return executeAndReturnImpl(task, fixedThreadPool);
    }

    /**
     * Выполнение задачи в отдельном потоке
     * и возвращение результата в переданный Consumer
     *
     * @param task     задача для выполнения
     * @param consumer куда будет передан результат выполненной задачи
     * @param <T>      тип результата
     */
    public <T> void executeAndReturn(Callable<T> task, Consumer<T> consumer) {
        fixedThreadPool.execute(() -> {
            consumer.accept(executeAndReturnImpl(task, fixedThreadPool));
        });
    }

    /**
     * Выполнение задачи в отдельном потоке
     * <p>
     * <b>Данный метод предназначен только для небольших задач</b>
     * </p>
     *
     * @param task задача для выполнения
     */
    public void executeShortTask(Runnable task) {
        cacheTreadPool.execute(new RunnableWrapper(task));
    }

    /**
     * Выполнение задачи в отдельном потоке с результатом выполнения
     * <p>
     * <b>Данный метод предназначен только для небольших задач</b>
     *
     * @param task задача для выполнения
     * @param <T>  тип результата выполняемой задачи
     * @return возвращает Future который, представляет задачу в процессе выполнения.
     */
    public <T> Future<T> executeShortTask( Callable<T> task )
    {

        if( task == null )
            return null;

        return cacheTreadPool.submit(task);
    }

    /**
     * Выполнение задачи в отдельном потоке с результатом выполнения.
     * <p>
     * В данном случае, результатом будет то, что было передано
     * вторым аргументом - <code>result</code>
     * </p>
     * <b>Данный метод предназначен только для небольших задач</b>
     *
     * @param task   задача для выполнения
     * @param result результат
     * @param <T>    тип результата
     * @return возвращает Future, который представляет задачу в процессе выполнения.
     */
    public <T> Future<? super T> executeShortTask(Runnable task, T result) {
        return cacheTreadPool.submit(new RunnableWrapper(task), result);
    }

    /**
     * Выполнение списка задач в отдельных потоках с результатом выполнения
     * <p>
     * <b>Метод заблокирует текущий поток</b>, поэтому если вы хотите выполнить несколько задач,
     * но при этом не мешать основному потоку - вызывайте данный метод в отдельном потоке
     * <p>К примеру вот так:</p>
     * <pre>
     *     ThreadPoolManager.getInstance().executeShortTask(() -> {
     *          final List<Future<String>> futures = ThreadPoolManager.getInstance().executeShortAllTask(callables);
     *     });
     * </pre>
     * <p>
     * <b>Данный метод предназначен только для небольших задач</b>
     *
     * @param callables список задач
     * @param <T>       тип результа выполняемых задач
     * @return возвращает список Future в котором каждый из них представляет задачу в процессе выполнения.
     * null если задачи были прерваны
     */
    public <T> List<Future<T>> executeShortAllTask(Collection<Callable<T>> callables) {
        try {
            return cacheTreadPool.invokeAll(callables);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    /**
     * <p>
     * Выполнение списка задач в отдельных потоках с результатом выполнения.
     * Можно задать timeout выполнения, он будет применен для каждой задачи в списке.
     * </p>
     * <b>Данный метод предназначен только для небольших задач</b>
     *
     * @param callables список задач
     * @param timeout   максимальное время ожидания
     * @param unit      time unit для timeout
     * @param <T>       тип результа выполняемых задач
     * @return возвращает список Future в котором каждый из них представляет задачу в процессе выполнения.
     * null если задачи были прерваны
     */
    public <T> List<Future<T>> executeShortAllTask(Collection<Callable<T>> callables, long timeout, TimeUnit unit) {
        try {
            return cacheTreadPool.invokeAll (callables, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    public String getStats() {
        return System.lineSeparator() + "STP:" + System.lineSeparator() +
                " + KernelScheduledPool:" + System.lineSeparator() +
                " |- ActiveThreads:   " + scheduledThreadPool.getActiveCount() + System.lineSeparator() +
                " |- PoolSize:        " + scheduledThreadPool.getCorePoolSize() + System.lineSeparator() +
                " |- MaximumPoolSize: " + scheduledThreadPool.getMaximumPoolSize() + System.lineSeparator() +
                " |- CompletedTasks:  " + scheduledThreadPool.getCompletedTaskCount() + System.lineSeparator() +
                " |- ScheduledTasks:  " + scheduledThreadPool.getQueue().size() + System.lineSeparator() +
                " | -------" + System.lineSeparator() +
                "TP:" + System.lineSeparator() +
                " + KernelFixedPool:" + System.lineSeparator() +
                " |- ActiveThreads:   " + fixedThreadPool.getActiveCount() + System.lineSeparator() +
                " |- getCorePoolSize: " + fixedThreadPool.getCorePoolSize() + System.lineSeparator() +
                " |- MaximumPoolSize: " + fixedThreadPool.getMaximumPoolSize() + System.lineSeparator() +
                " |- LargestPoolSize: " + fixedThreadPool.getLargestPoolSize() + System.lineSeparator() +
                " |- PoolSize:        " + fixedThreadPool.getPoolSize() + System.lineSeparator() +
                " |- CompletedTasks:  " + fixedThreadPool.getCompletedTaskCount() + System.lineSeparator() +
                " |- QueuedTasks:     " + fixedThreadPool.getQueue().size() + System.lineSeparator() +
                " | -------" + System.lineSeparator() +
                " + KernelCachedPool:" + System.lineSeparator() +
                " |- ActiveThreads:   " + cacheTreadPool.getActiveCount() + System.lineSeparator() +
                " |- getCorePoolSize: " + cacheTreadPool.getCorePoolSize() + System.lineSeparator() +
                " |- MaximumPoolSize: " + cacheTreadPool.getMaximumPoolSize() + System.lineSeparator() +
                " |- LargestPoolSize: " + cacheTreadPool.getLargestPoolSize() + System.lineSeparator() +
                " |- PoolSize:        " + cacheTreadPool.getPoolSize() + System.lineSeparator() +
                " |- CompletedTasks:  " + cacheTreadPool.getCompletedTaskCount() + System.lineSeparator() +
                " |- QueuedTasks:     " + cacheTreadPool.getQueue().size() + System.lineSeparator() +
                " | -------";
    }

    /** */
    public void shutdown() {

        shutdown = true;

        shutdownImpl(scheduledThreadPool);
        shutdownImpl(fixedThreadPool);
        shutdownImpl(cacheTreadPool);
    }

    /** */
    private void shutdownImpl(ExecutorService executorService) {

        executorService.shutdown();

        try {

            if(!executorService.awaitTermination(2, TimeUnit.SECONDS))  {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    /** */
    private <T> T executeAndReturnImpl( Callable<T> callable, ThreadPoolExecutor executor) {

        try {

            if( callable == null )
                return null;

            return executor.submit( callable ).get();

        } catch ( ExecutionException e) {
            Throwable cause = e.getCause();
            log.error("Error on execute callable: {}", callable.getClass().getName(), cause );
            if (cause instanceof Error ) throw (Error) cause;
            if (cause instanceof RuntimeException ) throw (RuntimeException) cause;
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on execute callable: " + callable.getClass().getName(), cause );
        }catch ( InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Interrupted on execute callable: " + callable.getClass().getName() );
        } catch (RejectedExecutionException e) {
            throw e;
        }
    }

    public boolean isShutdown() {
        return shutdown;
    }

    /*
    private static final class PurgeTask implements Runnable {

        private final ScheduledThreadPoolExecutor scheduledThreadPool;
        PurgeTask(ScheduledThreadPoolExecutor scheduledThreadPool) {
            this.scheduledThreadPool = scheduledThreadPool;
        }
        @Override
        public void run() {
            scheduledThreadPool.purge();
        }
    }
    */

    private static class PriorityThreadFactory implements ThreadFactory {
        private final int priority;
        private final String name;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private PriorityThreadFactory(String name, int priority) {
            this.priority = priority;
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread( runnable, name + "-" + threadNumber.getAndIncrement() );
            thread.setPriority(priority);
            return thread;
        }
    }

    /** */
    private static class DaemonThreadFactory implements ThreadFactory {
        private final String name;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private DaemonThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, name + "-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    /** */
    private static final class RunnableWrapper implements Runnable {

        private final Runnable runnable;

        RunnableWrapper(final Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public  void run() {
            try {
                runnable.run();
            } catch (final Throwable e) {
                log.error("Error on call RunnableWrapper for {}", runnable.getClass().getName(), e);
                throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on call CallableWrapper for " + runnable.getClass().getName(), e);
            }
        }
    }

}
