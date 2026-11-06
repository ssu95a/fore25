package ru.inversion.dataset.fx;
import javafx.scene.Node;
import javafx.util.Pair;
import ru.inversion.dataset.IFilterItem;
import ru.inversion.db.entity.RegisterEnum;
import ru.inversion.fx.form.lov.AbstractLovBase;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Один элемент, поле, которое показывается и вводится в диалоге фильтра таблицы, вызываемого по F7.
 *
 * {@inheritDoc}
 *
 * @author sulimoff
 */
public class F7FilterItem implements IFilterItem, Comparable<F7FilterItem> {

    /**
     * Часть на форме фильтра где будет располагаться компонент для ввода значений.
     */
    public enum TypeLocation {

        /** Поля из таблицы */
        TABLE,

        /** Поля из списка доп компонентов */
        INFO
    }

    @Override
    public ValueTypeEnum getValueType() {
        return valueType;
    }

    public void setValueType( ValueTypeEnum valueType ) {
        this.valueType = valueType;
    }

    /**
     * Признак, что в качестве значения предиката используется выражение на SQL.
     */
    private ValueTypeEnum valueType = IFilterItem.ValueTypeEnum.VALUE;

    /**
     * Значение предиката для column.
     */
    private Object value;

    /**
     * Столбец к которому применяется значение предиката.
     */
    private String column;

    /**
     * Тип столбца к которому применяется выражение.
     */
    private Class type;

    // UI поля
    /**
     * Пояснительный текст - label - который появляется в диалоге фильтра.
     */
    private String label;

    /**
     * Всплывающая подсказка на диалоге фильтра.
     */
    private String toolTip;

    /**
     * LOV для выбора значений на диалоге фильтра.
     */
    private AbstractLovBase lov;

    /**
     * Проверять ли введенное значение, при наличии лова
     */
    private boolean validateFromLov;

    /**
     * FX UI компонент для заведения значений на диалоге фильтра.
     */
    private Node control;

    /**
     * В какой части формы фильтра должен располагаться компонент ввода значения.
     */
    private TypeLocation typeLocation = TypeLocation.TABLE;

    /**
     * Связанный экземпляр фильтра.
     */
    private F7FilterItem proxyFor;

    /**
     * Порядковый номер в группе
     */
    private int orderInGroup;

    /**
     * Маска. Используется например в календаре
     */
    private String mask;

    /**
     * Используется ли поиск по индексу
     */
    private boolean indexSearchAllowed;

    /**
     * Режим регистра
     */
    private RegisterEnum caseSensitiveMode;

    /**
     * Вспомогательное значения контрола.
     */
    private Object controlValue;

    /**
     * Признак того, что выражение было искусственно сгенерено
     */
    private boolean generatedExpression;


    /** */
    private boolean ignore95LikeSym=false;

    /** */
    public Supplier<List<Pair<?,String>>> getFactoryList() {
        return factoryList;
    }

    /** */
    public void setFactoryList(Supplier<List<Pair<?, String>>> factoryList) {
        this.factoryList = factoryList;
    }

    /** Создает список элементов из которого осуществляется выбор */
    private Supplier< List<Pair<?,String>> > factoryList;

    public F7FilterItem() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumn() {
        return column;
    }

    /**
     * Устанавливает название столбца для выражения.
     *
     * @param column имя столбца
     */
    public void setColumn(String column) {
        this.column = column;
    }

    /**
     * "Fluent interface" версия setColumn.
     *
     * @param column имя столбца
     * @return this
     */
    public F7FilterItem column(String column) {
        setColumn(column);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue() {
        return value;
    }

    /**
     * Установка значения для столбца.
     *
     * @param value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * "Fluent interface" версия setValue.
     *
     * @param value значение для столбца
     * @return this
     *
     * @see #setValue
     */
    public F7FilterItem value(Object value) {
        setValue(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class getType() {
        return type;
    }

    /**
     * Устанавливает тип данных для значения.
     *
     * @param type
     */
    public void setType(Class type) {
        this.type = type;
    }

    /**
     * "Fluent interface" версия setValue.
     *
     * @param type значение для столбца
     * @return this
     */
    public F7FilterItem type(Class type) {
        setType(type);
        return this;
    }

    /**
     * Устанавливает признак, что значение - это выражение на SQL.
     *
     * @param expression {@code true } - выражение {@code false} - константа
     */
    public void setExpression( boolean expression ) {
        setValueType( !expression ? IFilterItem.ValueTypeEnum.VALUE : IFilterItem.ValueTypeEnum.EXPRESSION );
    }

    /**
     * "Fluent interface" версия setExpression.
     *
     * @param expr признак
     * @return this
     */
    public F7FilterItem expression(boolean expr) {
        setExpression(expr);
        return this;
    }

    /**
     * Получение пояснительного текста - Label.
     *
     * @return текст, {@code null если не установлен}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Установка пояснительного текста - Label.
     *
     * @param label текст
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * "Fluent interface" версия setLabel.
     *
     * @param label текст
     * @return this
     */
    public F7FilterItem label(String label) {
        setLabel(label);
        return this;
    }

    /**
     * Получение текста всплывающей подсказки.
     *
     * @return текст
     */
    public String getToolTip() {
        return toolTip;
    }

    /**
     * Установка текста всплывающей подсказки.
     *
     * @param toolTip текст
     */
    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    /**
     * "Fluent interface" версия setLabel.
     *
     * @param toolTip текст
     * @return this
     */
    public F7FilterItem toolTip(String toolTip) {
        setToolTip(toolTip);
        return this;
    }

    /**
     * Получение LOV для выбора значения.
     *
     * @return lov
     */
    public AbstractLovBase getLov() {
        return lov;
    }

    /**
     * Установка LOV для выбора значения.
     *
     * @param lov LOV для выбора значений, {@code null} если нужно очистить
     */
    public void setLov(AbstractLovBase lov) {
        this.lov = lov;
    }

    /**
     * "Fluent interface" версия setLov.
     *
     * @param lov lov для выбора значений
     * @return this
     */
    public F7FilterItem lov(AbstractLovBase lov) {
        setLov(lov);
        return this;
    }

    /**
     * Установить компонент для редактирования значения.
     * <p>
     * Возможна установка требуемого контроля для редактирования значения.
     * Этот компонент будет размещен на форме редактирования фильтра.
     *
     * Такое поведение только когда значение вводится в режиме "константы",
     * при режиме "выражение" любой компонент подменяется на текстовое поле.
     *
     * @param control UI компонент
     */
    public void setControl(Node control) {
        this.control = control;
    }

    /**
     * Получить компонент для редактирования значения. Установленный методом {@code setControl}
     *
     * @return компонент
     */
    public Node getControl() {
        return control;
    }

    /**
     * "Fluent interface" версия setControl.
     *
     * @param control компонент для заведения значений
     * @return this
     */
    public F7FilterItem control(Node control) {
        setControl(control);
        return this;
    }

    /**
     * Установить тип части формы, в которой будет размещаться компонент для ввода значения. Форма фильтра делиться на 2 части: в 1-ую попадают поля из таблицы. во 2-ую инфо поля.
     *
     * @param typeLocation в какую часть формы должен попасть компонент
     */
    public void setTypeLocation(TypeLocation typeLocation) {
        this.typeLocation = typeLocation;
    }

    /**
     * Получить тип части формы где будет компонент. Установленный методом setTypeLocation.
     *
     * @return тип части.
     */
    public TypeLocation getTypeLocation() {
        return typeLocation;
    }

    /**
     * "Fluent interface" версия setTypeLocation.
     *
     * @param typeLocation в какую часть формы должен попасть компонент
     * @return this
     */
    public F7FilterItem typeLocation(TypeLocation typeLocation) {
        setTypeLocation(typeLocation);
        return this;
    }

    /**
     *
     * @return
     */
    public F7FilterItem getProxyFor() {
        return proxyFor;
    }

    /**
     *
     * @param proxyFor
     */
    public void setProxyFor(F7FilterItem proxyFor) {
        this.proxyFor = proxyFor;
    }

    /**
     *
     * @param proxyFor
     * @return
     */
    public F7FilterItem proxyFor(F7FilterItem proxyFor) {
        setProxyFor(proxyFor);
        return this;
    }

    public int getOrderInGroup() {
        return orderInGroup;
    }

    public void setOrderInGroup(int orderInGroup) {
        this.orderInGroup = orderInGroup;
    }

    public F7FilterItem orderInGroup(int orderInGroup) {
        this.orderInGroup = orderInGroup;
        return this;
    }

    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }

    public F7FilterItem mask(String mask) {
        this.mask = mask;
        return this;
    }

    public boolean isIndexSearchAllowed() {
        return indexSearchAllowed;
    }

    public void setIndexSearchAllowed(boolean indexSearchAllowed) {
        this.indexSearchAllowed = indexSearchAllowed;
    }

    public F7FilterItem indexSearchAllowed(boolean indexSearchAllowed) {
        this.indexSearchAllowed = indexSearchAllowed;
        return this;
    }

    public boolean isValidateFromLov() {
        return validateFromLov;
    }

    public void setValidateFromLov(boolean validateFromLov) {
        this.validateFromLov = validateFromLov;
    }

    public F7FilterItem validateFromLov(boolean validateFromLov) {
        this.validateFromLov = validateFromLov;
        return this;
    }

    public RegisterEnum getCaseSensetiveMode() {
        return caseSensitiveMode;
    }

    public void setCaseSensetiveMode(RegisterEnum caseSensitiveMode ) {
        this.caseSensitiveMode = caseSensitiveMode;
    }

    public F7FilterItem caseSensetiveMode(RegisterEnum caseSensetiveMode) {
        this.caseSensitiveMode = caseSensetiveMode;
        return this;
    }

    public Object getControlValue() {
        return controlValue;
    }

    public void setControlValue(Object controlValue) {
        this.controlValue = controlValue;
    }

    /** */
    public boolean isGeneratedExpression() {
        return generatedExpression;
    }

    public void setGeneratedExpression(boolean generatedExpression) {
        this.generatedExpression = generatedExpression;
    }

    /** */
    public boolean isExpression() {
        return isGeneratedExpression() || getValueType() == ValueTypeEnum.EXPRESSION;
    }

    /** не интерпретировать символ _ как выражения like */
    @Override
    public boolean isIgnore95LikeSym() {
        return ignore95LikeSym;
    }
    public void setIgnore95LikeSym( boolean ignore95LikeSym ) {
        this.ignore95LikeSym = ignore95LikeSym;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.column);
        hash = 79 * hash + Objects.hashCode(this.typeLocation);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final F7FilterItem other = (F7FilterItem) obj;
        if (!Objects.equals(this.column, other.column)) {
            return false;
        }
        if (this.typeLocation != other.typeLocation) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(F7FilterItem o) {

        int result = Integer.compare(getOrderInGroup(), o.getOrderInGroup());
        if (result == 0) {
            return getColumn().compareToIgnoreCase(o.getColumn());
        } else {
            return result;
        }
    }

}
