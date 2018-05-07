package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import java.util.Arrays;
import java.util.Iterator;

public class ConstSizeArrayList<T> implements Iterable<T> {
    private static final int defaultSize = 4;
    private transient Object[] array;
    private int size;

    class MyIterator implements Iterator<T> {
        private int index = 0;

        MyIterator() {
        }

        public boolean hasNext() {
            return this.index < ConstSizeArrayList.this.size();
        }

        public T next() {
            ConstSizeArrayList constSizeArrayList = ConstSizeArrayList.this;
            int i = this.index;
            this.index = i + 1;
            return (T) constSizeArrayList.get(i);
        }

        public void remove() {
            throw new UnsupportedOperationException("not supported yet");
        }
    }

    public ConstSizeArrayList() {
        this.size = 4;
        this.array = new Object[this.size];
    }

    public ConstSizeArrayList(int capacity) {
        this.size = capacity;
        this.array = new Object[this.size];
    }

    public void add(int index, T obj) {
        if (index >= this.size || index < 0) {
            throwIndexOutOfBoundsException(index, this.size);
        }
        this.array[index] = obj;
    }

    public T remove(int index) {
        if (index >= this.size || index < 0) {
            throwIndexOutOfBoundsException(index, this.size);
        }
        return (T) this.array[index];
    }

    public T get(int index) {
        if (index >= this.size) {
            throwIndexOutOfBoundsException(index, this.size);
        }
        return (T) this.array[index];
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public void clear() {
        if (this.size != 0) {
            Arrays.fill(this.array, 0, this.size, null);
        }
    }

    public boolean remove(Object object) {
        Object[] a = this.array;
        int s = this.size;
        if (object != null) {
            for (int i = 0; i < s; i++) {
                if (object.equals(a[i])) {
                    a[i] = null;
                    return true;
                }
            }
        }
        return false;
    }

    static IndexOutOfBoundsException throwIndexOutOfBoundsException(int index, int size) {
        throw new IndexOutOfBoundsException("Invalid index " + index + ", size is " + size);
    }

    public Iterator<T> iterator() {
        return new MyIterator();
    }
}
