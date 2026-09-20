package ru.inversion.fx.form.property;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import ru.inversion.utils.S;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** */
public class BooleanProperties {

    /** */
    private BooleanProperties()
    { }

    /** */
    private static class NotBooleanProperty extends BooleanProperty {

        final private BooleanProperty source;

        private Map<InvalidationListener, InvalidationListener> invalidationMap;
        private Map<ChangeListener<? super Boolean>, ChangeListener<Boolean>> changeMap;

        private BooleanBinding boundBinding;

        private final Map<Property<Boolean>, BidirectionalLink> bidiLinks = new HashMap<>();

        private Map<ChangeListener<? super Boolean>, ChangeListener<Boolean>> changeMap() {
            return changeMap == null ? (changeMap = new HashMap<>()) : changeMap;
        }

        /** */
        private Map<InvalidationListener, InvalidationListener> invalidationMap() {
            return invalidationMap == null ? (invalidationMap = new HashMap<>()) : invalidationMap;
        }

        @Override
        public Object getBean() {
            return source.getBean();
        }
        @Override
        public String getName() {
            return  "not(" + ( !S.isNullOrEmpty(source.getName()) ? source.getName() : "unnamed") + ")";
        }

        /** */
        private NotBooleanProperty( BooleanProperty p )
        {
            this.source = Objects.requireNonNull( p, "'p' is null");
        }

        @Override
        public boolean get() {
            return !source.get();
        }

        @Override
        public void set( boolean value) {
            source.set(!value);
        }

        @Override
        public Boolean getValue() {
            return inv( source.getValue() );
        }

        @Override
        public void setValue(Boolean value) {
            source.setValue( value == null ? null : !value );
        }

        @Override
        public void bind(ObservableValue<? extends Boolean> observable) {

            Objects.requireNonNull( observable, "observable" );

            if( !bidiLinks.isEmpty() )
                throw new IllegalStateException("Cannot bind() while bidirectional links exist");

            // если source уже bound извне — это не наш bind
            if( source.isBound() && boundBinding == null )
                throw new IllegalStateException("Cannot bind(): source is already bound");

            // Если уже bound, сначала отвязываем
            if( isBound() )
                unbind();

            final BooleanBinding b;

            if( observable instanceof ObservableBooleanValue ) {
                b = Bindings.not( (ObservableBooleanValue) observable );
            }
            else
            {
                // NOT для Boolean (null-safe)
                b = Bindings.createBooleanBinding(() -> { Boolean v = observable.getValue(); return v == null ? false : !v; }, observable);
            }
            boundBinding = b;
            source.bind(boundBinding);
        }

        /** */
        @Override
        public void unbind() {

            if( boundBinding == null )
                return;  // мы не биндили -> не лезем

            // снимаем именно нашу связь
            source.unbind();

            boundBinding.dispose();
            boundBinding = null;
        }

        @Override
        public boolean isBound() {

            if( boundBinding != null && !source.isBound() )
            {
                boundBinding.dispose();
                boundBinding = null;
                return false;
            }

            return boundBinding != null && source.isBound();
        }

        /** */
        @Override
        public void bindBidirectional( Property<Boolean> other )
        {
            Objects.requireNonNull(other, "other");

            if( other == source )
                throw new IllegalArgumentException("Cannot bindBidirectional not(source) with source");

            if( other instanceof NotBooleanProperty && ((NotBooleanProperty) other).source == this.source )
                throw new IllegalArgumentException("Cannot bindBidirectional not(source) with not(source) of same source");


            if( this.isBound() || other.isBound() )
                throw new IllegalStateException("Cannot bindBidirectional when one side is bound");

            if (source.isBound())
                throw new IllegalStateException("Cannot bindBidirectional when source is bound");

            if( bidiLinks.containsKey(other) )
                return;

            BidirectionalLink link = new BidirectionalLink(other);
            bidiLinks.put( other, link );
            link.attach();

            // initial sync: other = !source
            link.syncOtherFromSource();
        }

        /** */
        @Override
        public void unbindBidirectional( Property<Boolean> other ) {

            if( other == null)
                return;

            BidirectionalLink link = bidiLinks.remove(other);
            if( link != null )
                link.detach();
        }

        /** */
        @Override
        public void addListener(InvalidationListener listener) {

            Objects.requireNonNull(listener, "listener");

            if( invalidationMap().containsKey(listener) )
                return;

            InvalidationListener wrapper = obs -> listener.invalidated(this);
            invalidationMap.put( listener, wrapper );
            source.addListener(wrapper);
        }

        /** */
        @Override
        public void removeListener(InvalidationListener listener) {

            if( invalidationMap == null || listener == null )
                return;

            InvalidationListener wrapper = invalidationMap.remove(listener);
            if( wrapper != null )
                source.removeListener(wrapper);

            if( invalidationMap.isEmpty() )
                invalidationMap = null;
        }

        @Override
        public void addListener( ChangeListener<? super Boolean> listener ) {

            Objects.requireNonNull(listener, "listener");

            if( changeMap().containsKey(listener) )
                return;

            ChangeListener<Boolean> wrapper = (obs, o, n) ->listener.changed(this, inv(o), inv(n));

            changeMap.put(listener, wrapper);
            source.addListener(wrapper);
        }

        @Override
        public void removeListener(ChangeListener<? super Boolean> listener) {

            if( changeMap == null || listener == null )
                return;

            ChangeListener<Boolean> wrapper = changeMap.remove(listener);

            if( wrapper != null )
                source.removeListener(wrapper);

            if( changeMap.isEmpty() )
                changeMap = null;
        }

        /** */
        public void dispose() {

            unbind( );

            for( BidirectionalLink link : bidiLinks.values() ) {
                link.detach();
            }

            bidiLinks.clear( );

            if( changeMap != null )
            {
                for( ChangeListener<Boolean> w : changeMap.values() ) {
                    source.removeListener(w);
                }
                changeMap.clear();
                changeMap = null;
            }

            if( invalidationMap != null )
            {
                for (InvalidationListener w : invalidationMap.values()) {
                    source.removeListener(w);
                }
                invalidationMap.clear();
                invalidationMap = null;
            }
        }

        /** */
        private static Boolean inv(Boolean b) { return b == null ? null : !b; }

        @Override
        public String toString() {
            return "NotBooleanProperty{value=" + get() + ", source=" + source + ", name='" + getName() + "'" + "}";
        }

        /** Ручной bidirectional: other <-> not(source) */
        private final class BidirectionalLink {

            private final Property<Boolean> other;
            private boolean updating;

            private final ChangeListener<Boolean> sourceListener = (obs, o, n) -> {

                if(updating)
                    return;

                updating = true;

                try {
                    syncOtherFromSource();
                } finally {
                    updating = false;
                }
            };

            private final ChangeListener<Boolean> otherListener = (obs, o, n) -> {

                if( updating )
                    return;

                updating = true;

                try {
                    syncSourceFromOther();
                } finally {
                    updating = false;
                }
            };

            private BidirectionalLink(Property<Boolean> other) {
                this.other = other;
            }

            private void attach() {
                source.addListener(sourceListener);
                other.addListener(otherListener );
            }

            private void detach() {
                source.removeListener(sourceListener);
                other.removeListener(otherListener );
            }

            private void syncOtherFromSource() {
                Boolean v = source.getValue();
                other.setValue(v == null ? null : !v);
            }

            private void syncSourceFromOther() {
                Boolean v = other.getValue();
                source.setValue(v == null ? null : !v);
            }
        }

    }

    /**
     * Создает свойство, которое всегда равно !source.
     */
    public static BooleanProperty not( BooleanProperty source ) {
        return new NotBooleanProperty(source);
    }

    /**
     * Создает read-only BooleanProperty
     */
    public static ReadOnlyBooleanProperty readOnly( ObservableBooleanValue source) {
        ReadOnlyBooleanWrapper wrapper = new ReadOnlyBooleanWrapper();
        wrapper.bind(source);
        return wrapper.getReadOnlyProperty();
    }

}
