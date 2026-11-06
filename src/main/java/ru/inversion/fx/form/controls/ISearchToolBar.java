package ru.inversion.fx.form.controls;

public interface ISearchToolBar {

    /**
     * Ищет и маркирует найденный текст
     *
     * @param searchText искомый текст
     * @return кол-во найденных элементов
     */
    public int searchAndMark(String searchText);

    /**
     * Очищает маркировку
     */
    public void clearMark();

}
