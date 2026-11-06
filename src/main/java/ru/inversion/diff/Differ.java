package ru.inversion.diff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.diff.access.PropertyAccessor;
import ru.inversion.diff.comparison.ComparisonService;
import ru.inversion.diff.comparison.ComparisonStrategy;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.tc.TaskContext;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public class Differ {

    private static final Logger LOG = LoggerFactory.getLogger(Differ.class);

    private final List<DiffItem> diffItemList = new ArrayList<>();

    private final ComparisonService comparisonService = new ComparisonService();

    private final TaskContext taskContext;

    private final ViewContext viewContext;

    private final Consumer<List<DiffItem>> resultConsumer;

    public Differ(Consumer<List<DiffItem>> resultConsumer) {
        this(null, null, resultConsumer);
    }

    public Differ(ViewContext viewContext, Consumer<List<DiffItem>> resultConsumer) {
        this(null, viewContext, resultConsumer);
    }

    public Differ(TaskContext taskContext, ViewContext viewContext, Consumer<List<DiffItem>> resultConsumer) {
        this.taskContext = taskContext;
        this.viewContext = viewContext;
        this.resultConsumer = resultConsumer;
    }

    public void addItem(DiffItem diffItem) {
        diffItemList.add(diffItem);
    }

    public void addAllItem(Collection<DiffItem> diffItemCollection) {
        diffItemList.addAll(diffItemCollection);
    }

    public void addAllItem(DiffItem... diffItems) {
        diffItemList.addAll(Arrays.asList(diffItems));
    }

    public List<DiffItemResult> compare() {
        final List<DiffItemResult> checkedItemList = internalCompare();
        Collections.sort(checkedItemList);
        return checkedItemList;
    }

    public void compareAndShow() {
        final List<DiffItemResult> checkedItemList = internalCompare();
        Collections.sort(checkedItemList);
        final DiffParamObject diffParamObject = new DiffParamObject(checkedItemList, resultConsumer);
        new FXFormLauncher<>(taskContext, viewContext, "ru/inversion/fx/form/fxml/DiffView.fxml")
                .dialogMode(AbstractBaseController.FormModeEnum.VM_SHOW)
                .bundle(ResourceBundle.getBundle("fore"))
                .dataObject(diffParamObject)
                .modal(true)
                .show();
    }

    private List<DiffItemResult> internalCompare() {
        final List<DiffItemResult> checkedItemList = new ArrayList<>(diffItemList.size());
        for (DiffItem diffItem : diffItemList) {
            final ComparisonStrategy comparisonStrategy = comparisonService.resolveComparisonStrategy(diffItem);
            if (comparisonStrategy == null) {
                throw new IllegalStateException(String.format("Unsupported type: %s", diffItem.getValueType()));
            }
            final DiffState state = comparisonStrategy.compare(diffItem);

            try {
                final DiffItemInfo diffItemInfo = introspectEntity(diffItem.getClass());
                checkedItemList.add(new DiffItemResult(diffItem, state, diffItemInfo));
            } catch (IntrospectionException e) {
                LOG.error("Can't introspect class", e);
            }
        }
        return checkedItemList;
    }

    private DiffItemInfo introspectEntity(Class<?> entity) throws IntrospectionException {
        final DiffItemInfo diffItemInfo = new DiffItemInfo(entity);
        final PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(entity).getPropertyDescriptors();
        for (PropertyDescriptor descriptor : propertyDescriptors) {
            final String propertyName = descriptor.getName();
            final Method readMethod = descriptor.getReadMethod();
            final Method writeMethod = descriptor.getWriteMethod();
            final PropertyAccessor propertyAccessor = new PropertyAccessor(propertyName, readMethod, writeMethod);
            diffItemInfo.addPropertyAccessor(propertyAccessor);
        }
        return diffItemInfo;
    }

}
