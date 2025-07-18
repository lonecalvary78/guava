/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ObjectArrays.checkElementsNotNull;
import static java.lang.System.arraycopy;
import static java.util.Arrays.sort;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * A {@link NavigableSet} whose contents will never change, with many other important properties
 * detailed at {@link ImmutableCollection}.
 *
 * <p><b>Warning:</b> as with any sorted collection, you are strongly advised not to use a {@link
 * Comparator} or {@link Comparable} type whose comparison behavior is <i>inconsistent with
 * equals</i>. That is, {@code a.compareTo(b)} or {@code comparator.compare(a, b)} should equal zero
 * <i>if and only if</i> {@code a.equals(b)}. If this advice is not followed, the resulting
 * collection will not correctly obey its specification.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained">immutable collections</a>.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 2.0 (implements {@code NavigableSet} since 12.0)
 */
// TODO(benyu): benchmark and optimize all creation paths, which are a mess now
@GwtCompatible(emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableSortedSet<E> extends ImmutableSet<E>
    implements NavigableSet<E>, SortedIterable<E> {
  /**
   * Returns a {@code Collector} that accumulates the input elements into a new {@code
   * ImmutableSortedSet}, ordered by the specified comparator.
   *
   * <p>If the elements contain duplicates (according to the comparator), only the first duplicate
   * in encounter order will appear in the result.
   *
   * @since 33.2.0 (available since 21.0 in guava-jre)
   */
  @IgnoreJRERequirement // Users will use this only if they're already using streams.
  public static <E> Collector<E, ?, ImmutableSortedSet<E>> toImmutableSortedSet(
      Comparator<? super E> comparator) {
    return CollectCollectors.toImmutableSortedSet(comparator);
  }

  static <E> RegularImmutableSortedSet<E> emptySet(Comparator<? super E> comparator) {
    if (Ordering.natural().equals(comparator)) {
      @SuppressWarnings("unchecked") // The natural-ordered empty set supports all types.
      RegularImmutableSortedSet<E> result =
          (RegularImmutableSortedSet<E>) RegularImmutableSortedSet.NATURAL_EMPTY_SET;
      return result;
    } else {
      return new RegularImmutableSortedSet<>(ImmutableList.of(), comparator);
    }
  }

  /**
   * Returns the empty immutable sorted set.
   *
   * <p><b>Performance note:</b> the instance returned is a singleton.
   */
  @SuppressWarnings("unchecked") // The natural-ordered empty set supports all types.
  public static <E> ImmutableSortedSet<E> of() {
    return (ImmutableSortedSet<E>) RegularImmutableSortedSet.NATURAL_EMPTY_SET;
  }

  /** Returns an immutable sorted set containing a single element. */
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(E e1) {
    return new RegularImmutableSortedSet<>(ImmutableList.of(e1), Ordering.natural());
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by their natural ordering.
   * When multiple elements are equivalent according to {@link Comparable#compareTo}, only the first
   * one specified is included.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(E e1, E e2) {
    return construct(Ordering.natural(), 2, e1, e2);
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by their natural ordering.
   * When multiple elements are equivalent according to {@link Comparable#compareTo}, only the first
   * one specified is included.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(E e1, E e2, E e3) {
    return construct(Ordering.natural(), 3, e1, e2, e3);
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by their natural ordering.
   * When multiple elements are equivalent according to {@link Comparable#compareTo}, only the first
   * one specified is included.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(E e1, E e2, E e3, E e4) {
    return construct(Ordering.natural(), 4, e1, e2, e3, e4);
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by their natural ordering.
   * When multiple elements are equivalent according to {@link Comparable#compareTo}, only the first
   * one specified is included.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(
      E e1, E e2, E e3, E e4, E e5) {
    return construct(Ordering.natural(), 5, e1, e2, e3, e4, e5);
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by their natural ordering.
   * When multiple elements are equivalent according to {@link Comparable#compareTo}, only the first
   * one specified is included.
   *
   * @throws NullPointerException if any element is null
   * @since 3.0 (source-compatible since 2.0)
   */
  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E... remaining) {
    Comparable<?>[] contents = new Comparable<?>[6 + remaining.length];
    contents[0] = e1;
    contents[1] = e2;
    contents[2] = e3;
    contents[3] = e4;
    contents[4] = e5;
    contents[5] = e6;
    arraycopy(remaining, 0, contents, 6, remaining.length);
    return construct(Ordering.natural(), contents.length, (E[]) contents);
  }

  // TODO(kevinb): Consider factory methods that reject duplicates

  /**
   * Returns an immutable sorted set containing the given elements sorted by their natural ordering.
   * When multiple elements are equivalent according to {@link Comparable#compareTo}, only the first
   * one specified is included.
   *
   * @throws NullPointerException if any of {@code elements} is null
   * @since 3.0
   */
  public static <E extends Comparable<? super E>> ImmutableSortedSet<E> copyOf(E[] elements) {
    return construct(Ordering.natural(), elements.length, elements.clone());
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by their natural ordering.
   * When multiple elements are equivalent according to {@code compareTo()}, only the first one
   * specified is included. To create a copy of a {@code SortedSet} that preserves the comparator,
   * call {@link #copyOfSorted} instead. This method iterates over {@code elements} at most once.
   *
   * <p>Note that if {@code s} is a {@code Set<String>}, then {@code ImmutableSortedSet.copyOf(s)}
   * returns an {@code ImmutableSortedSet<String>} containing each of the strings in {@code s},
   * while {@code ImmutableSortedSet.of(s)} returns an {@code ImmutableSortedSet<Set<String>>}
   * containing one element (the given set itself).
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * <p>This method is not type-safe, as it may be called on elements that are not mutually
   * comparable.
   *
   * @throws ClassCastException if the elements are not mutually comparable
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E> ImmutableSortedSet<E> copyOf(Iterable<? extends E> elements) {
    // Hack around E not being a subtype of Comparable.
    // Unsafe, see ImmutableSortedSetFauxverideShim.
    @SuppressWarnings("unchecked")
    Ordering<E> naturalOrder = (Ordering<E>) Ordering.<Comparable<?>>natural();
    return copyOf(naturalOrder, elements);
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by their natural ordering.
   * When multiple elements are equivalent according to {@code compareTo()}, only the first one
   * specified is included. To create a copy of a {@code SortedSet} that preserves the comparator,
   * call {@link #copyOfSorted} instead. This method iterates over {@code elements} at most once.
   *
   * <p>Note that if {@code s} is a {@code Set<String>}, then {@code ImmutableSortedSet.copyOf(s)}
   * returns an {@code ImmutableSortedSet<String>} containing each of the strings in {@code s},
   * while {@code ImmutableSortedSet.of(s)} returns an {@code ImmutableSortedSet<Set<String>>}
   * containing one element (the given set itself).
   *
   * <p><b>Note:</b> Despite what the method name suggests, if {@code elements} is an {@code
   * ImmutableSortedSet}, it may be returned instead of a copy.
   *
   * <p>This method is not type-safe, as it may be called on elements that are not mutually
   * comparable.
   *
   * <p>This method is safe to use even when {@code elements} is a synchronized or concurrent
   * collection that is currently being modified by another thread.
   *
   * @throws ClassCastException if the elements are not mutually comparable
   * @throws NullPointerException if any of {@code elements} is null
   * @since 7.0 (source-compatible since 2.0)
   */
  public static <E> ImmutableSortedSet<E> copyOf(Collection<? extends E> elements) {
    // Hack around E not being a subtype of Comparable.
    // Unsafe, see ImmutableSortedSetFauxverideShim.
    @SuppressWarnings("unchecked")
    Ordering<E> naturalOrder = (Ordering<E>) Ordering.<Comparable<?>>natural();
    return copyOf(naturalOrder, elements);
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by their natural ordering.
   * When multiple elements are equivalent according to {@code compareTo()}, only the first one
   * specified is included.
   *
   * <p>This method is not type-safe, as it may be called on elements that are not mutually
   * comparable.
   *
   * @throws ClassCastException if the elements are not mutually comparable
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E> ImmutableSortedSet<E> copyOf(Iterator<? extends E> elements) {
    // Hack around E not being a subtype of Comparable.
    // Unsafe, see ImmutableSortedSetFauxverideShim.
    @SuppressWarnings("unchecked")
    Ordering<E> naturalOrder = (Ordering<E>) Ordering.<Comparable<?>>natural();
    return copyOf(naturalOrder, elements);
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by the given {@code
   * Comparator}. When multiple elements are equivalent according to {@code compareTo()}, only the
   * first one specified is included.
   *
   * @throws NullPointerException if {@code comparator} or any of {@code elements} is null
   */
  public static <E> ImmutableSortedSet<E> copyOf(
      Comparator<? super E> comparator, Iterator<? extends E> elements) {
    return new Builder<E>(comparator).addAll(elements).build();
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by the given {@code
   * Comparator}. When multiple elements are equivalent according to {@code compare()}, only the
   * first one specified is included. This method iterates over {@code elements} at most once.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * @throws NullPointerException if {@code comparator} or any of {@code elements} is null
   */
  public static <E> ImmutableSortedSet<E> copyOf(
      Comparator<? super E> comparator, Iterable<? extends E> elements) {
    checkNotNull(comparator);
    boolean hasSameComparator = SortedIterables.hasSameComparator(comparator, elements);

    if (hasSameComparator && (elements instanceof ImmutableSortedSet)) {
      @SuppressWarnings("unchecked")
      ImmutableSortedSet<E> original = (ImmutableSortedSet<E>) elements;
      if (!original.isPartialView()) {
        return original;
      }
    }
    @SuppressWarnings("unchecked") // elements only contains E's; it's safe.
    E[] array = (E[]) Iterables.toArray(elements);
    return construct(comparator, array.length, array);
  }

  /**
   * Returns an immutable sorted set containing the given elements sorted by the given {@code
   * Comparator}. When multiple elements are equivalent according to {@code compareTo()}, only the
   * first one specified is included.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * <p>This method is safe to use even when {@code elements} is a synchronized or concurrent
   * collection that is currently being modified by another thread.
   *
   * @throws NullPointerException if {@code comparator} or any of {@code elements} is null
   * @since 7.0 (source-compatible since 2.0)
   */
  public static <E> ImmutableSortedSet<E> copyOf(
      Comparator<? super E> comparator, Collection<? extends E> elements) {
    return copyOf(comparator, (Iterable<? extends E>) elements);
  }

  /**
   * Returns an immutable sorted set containing the elements of a sorted set, sorted by the same
   * {@code Comparator}. That behavior differs from {@link #copyOf(Iterable)}, which always uses the
   * natural ordering of the elements.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * <p>This method is safe to use even when {@code sortedSet} is a synchronized or concurrent
   * collection that is currently being modified by another thread.
   *
   * @throws NullPointerException if {@code sortedSet} or any of its elements is null
   */
  public static <E> ImmutableSortedSet<E> copyOfSorted(SortedSet<E> sortedSet) {
    Comparator<? super E> comparator = SortedIterables.comparator(sortedSet);
    ImmutableList<E> list = ImmutableList.copyOf(sortedSet);
    if (list.isEmpty()) {
      return emptySet(comparator);
    } else {
      return new RegularImmutableSortedSet<>(list, comparator);
    }
  }

  /**
   * Constructs an {@code ImmutableSortedSet} from the first {@code n} elements of {@code contents}.
   * If {@code k} is the size of the returned {@code ImmutableSortedSet}, then the sorted unique
   * elements are in the first {@code k} positions of {@code contents}, and {@code contents[i] ==
   * null} for {@code k <= i < n}.
   *
   * <p>This method takes ownership of {@code contents}; do not modify {@code contents} after this
   * returns.
   *
   * @throws NullPointerException if any of the first {@code n} elements of {@code contents} is null
   */
  static <E> ImmutableSortedSet<E> construct(
      Comparator<? super E> comparator, int n, E... contents) {
    if (n == 0) {
      return emptySet(comparator);
    }
    checkElementsNotNull(contents, n);
    sort(contents, 0, n, comparator);
    int uniques = 1;
    for (int i = 1; i < n; i++) {
      E cur = contents[i];
      E prev = contents[uniques - 1];
      if (comparator.compare(cur, prev) != 0) {
        contents[uniques++] = cur;
      }
    }
    Arrays.fill(contents, uniques, n, null);
    if (uniques < contents.length / 2) {
      // Deduplication eliminated many of the elements.  We don't want to retain an arbitrarily
      // large array relative to the number of elements, so we cap the ratio.
      contents = Arrays.copyOf(contents, uniques);
    }
    return new RegularImmutableSortedSet<E>(
        ImmutableList.<E>asImmutableList(contents, uniques), comparator);
  }

  /**
   * Returns a builder that creates immutable sorted sets with an explicit comparator. If the
   * comparator has a more general type than the set being generated, such as creating a {@code
   * SortedSet<Integer>} with a {@code Comparator<Number>}, use the {@link Builder} constructor
   * instead.
   *
   * @throws NullPointerException if {@code comparator} is null
   */
  public static <E> Builder<E> orderedBy(Comparator<E> comparator) {
    return new Builder<>(comparator);
  }

  /**
   * Returns a builder that creates immutable sorted sets whose elements are ordered by the reverse
   * of their natural ordering.
   */
  public static <E extends Comparable<?>> Builder<E> reverseOrder() {
    return new Builder<>(Collections.reverseOrder());
  }

  /**
   * Returns a builder that creates immutable sorted sets whose elements are ordered by their
   * natural ordering. The sorted sets use {@link Ordering#natural()} as the comparator. This method
   * provides more type-safety than {@link #builder}, as it can be called only for classes that
   * implement {@link Comparable}.
   */
  public static <E extends Comparable<?>> Builder<E> naturalOrder() {
    return new Builder<>(Ordering.natural());
  }

  /**
   * A builder for creating immutable sorted set instances, especially {@code public static final}
   * sets ("constant sets"), with a given comparator. Example:
   *
   * {@snippet :
   * public static final ImmutableSortedSet<Number> LUCKY_NUMBERS =
   *     new ImmutableSortedSet.Builder<Number>(ODDS_FIRST_COMPARATOR)
   *         .addAll(SINGLE_DIGIT_PRIMES)
   *         .add(42)
   *         .build();
   * }
   *
   * <p>Builder instances can be reused; it is safe to call {@link #build} multiple times to build
   * multiple sets in series. Each set is a superset of the set created before it.
   *
   * @since 2.0
   */
  public static final class Builder<E> extends ImmutableSet.Builder<E> {
    private final Comparator<? super E> comparator;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder generated by {@link
     * ImmutableSortedSet#orderedBy}.
     */
    /*
     * TODO(cpovirk): use Object[] instead of E[] in the mainline? (The backport is different and
     * doesn't need this suppression, but we keep it to minimize diffs.) Generally be more clear
     * about when we have an Object[] vs. a Comparable[] or other array type in internalArray? If we
     * used Object[], we might be able to optimize toArray() to use clone() sometimes. (See
     * cl/592273615 and cl/592273683.)
     */
    public Builder(Comparator<? super E> comparator) {
      this.comparator = checkNotNull(comparator);
    }

    Builder(Comparator<? super E> comparator, int expectedKeys) {
      super(expectedKeys, false);
      this.comparator = checkNotNull(comparator);
    }

    /**
     * Adds {@code element} to the {@code ImmutableSortedSet}. If the {@code ImmutableSortedSet}
     * already contains {@code element}, then {@code add} has no effect. (only the previously added
     * element is retained).
     *
     * @param element the element to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code element} is null
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> add(E element) {
      super.add(element);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSortedSet}, ignoring duplicate
     * elements (only the first duplicate element is added).
     *
     * @param elements the elements to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} contains a null element
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> add(E... elements) {
      super.add(elements);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSortedSet}, ignoring duplicate
     * elements (only the first duplicate element is added).
     *
     * @param elements the elements to add to the {@code ImmutableSortedSet}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} contains a null element
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> addAll(Iterable<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSortedSet}, ignoring duplicate
     * elements (only the first duplicate element is added).
     *
     * @param elements the elements to add to the {@code ImmutableSortedSet}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} contains a null element
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> addAll(Iterator<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    Builder<E> combine(ImmutableSet.Builder<E> builder) {
      super.combine(builder);
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableSortedSet} based on the contents of the {@code
     * Builder} and its comparator.
     */
    @Override
    public ImmutableSortedSet<E> build() {
      @SuppressWarnings("unchecked") // we're careful to put only E's in here
      E[] contentsArray = (E[]) contents;
      ImmutableSortedSet<E> result = construct(comparator, size, contentsArray);
      this.size = result.size(); // we eliminated duplicates in-place in contentsArray
      this.forceCopy = true;
      return result;
    }
  }

  int unsafeCompare(Object a, @Nullable Object b) {
    return unsafeCompare(comparator, a, b);
  }

  static int unsafeCompare(Comparator<?> comparator, Object a, @Nullable Object b) {
    // Pretend the comparator can compare anything. If it turns out it can't
    // compare a and b, we should get a CCE or NPE on the subsequent line. Only methods
    // that are spec'd to throw CCE and NPE should call this.
    @SuppressWarnings({"unchecked", "nullness"})
    Comparator<@Nullable Object> unsafeComparator = (Comparator<@Nullable Object>) comparator;
    return unsafeComparator.compare(a, b);
  }

  final transient Comparator<? super E> comparator;

  ImmutableSortedSet(Comparator<? super E> comparator) {
    this.comparator = comparator;
  }

  /**
   * Returns the comparator that orders the elements, which is {@link Ordering#natural()} when the
   * natural ordering of the elements is used. Note that its behavior is not consistent with {@link
   * SortedSet#comparator()}, which returns {@code null} to indicate natural ordering.
   */
  @Override
  public Comparator<? super E> comparator() {
    return comparator;
  }

  @Override // needed to unify the iterator() methods in Collection and SortedIterable
  public abstract UnmodifiableIterator<E> iterator();

  /**
   * {@inheritDoc}
   *
   * <p>This method returns a serializable {@code ImmutableSortedSet}.
   *
   * <p>The {@link SortedSet#headSet} documentation states that a subset of a subset throws an
   * {@link IllegalArgumentException} if passed a {@code toElement} greater than an earlier {@code
   * toElement}. However, this method doesn't throw an exception in that situation, but instead
   * keeps the original {@code toElement}.
   */
  @Override
  public ImmutableSortedSet<E> headSet(E toElement) {
    return headSet(toElement, false);
  }

  /**
   * @since 12.0
   */
  @Override
  public ImmutableSortedSet<E> headSet(E toElement, boolean inclusive) {
    return headSetImpl(checkNotNull(toElement), inclusive);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This method returns a serializable {@code ImmutableSortedSet}.
   *
   * <p>The {@link SortedSet#subSet} documentation states that a subset of a subset throws an {@link
   * IllegalArgumentException} if passed a {@code fromElement} smaller than an earlier {@code
   * fromElement}. However, this method doesn't throw an exception in that situation, but instead
   * keeps the original {@code fromElement}. Similarly, this method keeps the original {@code
   * toElement}, instead of throwing an exception, if passed a {@code toElement} greater than an
   * earlier {@code toElement}.
   */
  @Override
  public ImmutableSortedSet<E> subSet(E fromElement, E toElement) {
    return subSet(fromElement, true, toElement, false);
  }

  /**
   * @since 12.0
   */
  @GwtIncompatible // NavigableSet
  @Override
  public ImmutableSortedSet<E> subSet(
      E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
    checkNotNull(fromElement);
    checkNotNull(toElement);
    checkArgument(comparator.compare(fromElement, toElement) <= 0);
    return subSetImpl(fromElement, fromInclusive, toElement, toInclusive);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This method returns a serializable {@code ImmutableSortedSet}.
   *
   * <p>The {@link SortedSet#tailSet} documentation states that a subset of a subset throws an
   * {@link IllegalArgumentException} if passed a {@code fromElement} smaller than an earlier {@code
   * fromElement}. However, this method doesn't throw an exception in that situation, but instead
   * keeps the original {@code fromElement}.
   */
  @Override
  public ImmutableSortedSet<E> tailSet(E fromElement) {
    return tailSet(fromElement, true);
  }

  /**
   * @since 12.0
   */
  @Override
  public ImmutableSortedSet<E> tailSet(E fromElement, boolean inclusive) {
    return tailSetImpl(checkNotNull(fromElement), inclusive);
  }

  /*
   * These methods perform most headSet, subSet, and tailSet logic, besides
   * parameter validation.
   */
  abstract ImmutableSortedSet<E> headSetImpl(E toElement, boolean inclusive);

  abstract ImmutableSortedSet<E> subSetImpl(
      E fromElement, boolean fromInclusive, E toElement, boolean toInclusive);

  abstract ImmutableSortedSet<E> tailSetImpl(E fromElement, boolean inclusive);

  /**
   * @since 12.0
   */
  @GwtIncompatible // NavigableSet
  @Override
  public @Nullable E lower(E e) {
    return Iterators.<@Nullable E>getNext(headSet(e, false).descendingIterator(), null);
  }

  /**
   * @since 12.0
   */
  @Override
  public @Nullable E floor(E e) {
    return Iterators.<@Nullable E>getNext(headSet(e, true).descendingIterator(), null);
  }

  /**
   * @since 12.0
   */
  @Override
  public @Nullable E ceiling(E e) {
    return Iterables.<@Nullable E>getFirst(tailSet(e, true), null);
  }

  /**
   * @since 12.0
   */
  @GwtIncompatible // NavigableSet
  @Override
  public @Nullable E higher(E e) {
    return Iterables.<@Nullable E>getFirst(tailSet(e, false), null);
  }

  @Override
  public E first() {
    return iterator().next();
  }

  @Override
  public E last() {
    return descendingIterator().next();
  }

  /**
   * Guaranteed to throw an exception and leave the set unmodified.
   *
   * @since 12.0
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @GwtIncompatible // NavigableSet
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  public final @Nullable E pollFirst() {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the set unmodified.
   *
   * @since 12.0
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @GwtIncompatible // NavigableSet
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  public final @Nullable E pollLast() {
    throw new UnsupportedOperationException();
  }

  @GwtIncompatible // NavigableSet
  @LazyInit
  transient @Nullable ImmutableSortedSet<E> descendingSet;

  /**
   * @since 12.0
   */
  @GwtIncompatible // NavigableSet
  @Override
  public ImmutableSortedSet<E> descendingSet() {
    // racy single-check idiom
    ImmutableSortedSet<E> result = descendingSet;
    if (result == null) {
      result = descendingSet = createDescendingSet();
      result.descendingSet = this;
    }
    return result;
  }

  // Most classes should implement this as new DescendingImmutableSortedSet<E>(this),
  // but we push down that implementation because ProGuard can't eliminate it even when it's always
  // overridden.
  @GwtIncompatible // NavigableSet
  abstract ImmutableSortedSet<E> createDescendingSet();

  /**
   * @since 12.0
   */
  @GwtIncompatible // NavigableSet
  @Override
  public abstract UnmodifiableIterator<E> descendingIterator();

  /** Returns the position of an element within the set, or -1 if not present. */
  abstract int indexOf(@Nullable Object target);

  /*
   * This class is used to serialize all ImmutableSortedSet instances,
   * regardless of implementation type. It captures their "logical contents"
   * only. This is necessary to ensure that the existence of a particular
   * implementation type is an implementation detail.
   */
  @J2ktIncompatible // serialization
  private static final class SerializedForm<E> implements Serializable {
    final Comparator<? super E> comparator;
    final Object[] elements;

    SerializedForm(Comparator<? super E> comparator, Object[] elements) {
      this.comparator = comparator;
      this.elements = elements;
    }

    @SuppressWarnings("unchecked")
    Object readResolve() {
      return new Builder<E>(comparator).add((E[]) elements).build();
    }

    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
  }

  @J2ktIncompatible // serialization
  private void readObject(ObjectInputStream unused) throws InvalidObjectException {
    throw new InvalidObjectException("Use SerializedForm");
  }

  @Override
  @J2ktIncompatible // serialization
  Object writeReplace() {
    return new SerializedForm<E>(comparator, toArray());
  }

  /**
   * Not supported. Use {@link #toImmutableSortedSet} instead. This method exists only to hide
   * {@link ImmutableSet#toImmutableSet} from consumers of {@code ImmutableSortedSet}.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link ImmutableSortedSet#toImmutableSortedSet}.
   * @since 33.2.0 (available since 21.0 in guava-jre)
   */
  @DoNotCall("Use toImmutableSortedSet")
  @Deprecated
  @IgnoreJRERequirement // Users will use this only if they're already using streams.
  public static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. Use {@link #naturalOrder}, which offers better type-safety, instead. This method
   * exists only to hide {@link ImmutableSet#builder} from consumers of {@code ImmutableSortedSet}.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link ImmutableSortedSet#naturalOrder}, which offers better type-safety.
   */
  @DoNotCall("Use naturalOrder")
  @Deprecated
  public static <E> ImmutableSortedSet.Builder<E> builder() {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. This method exists only to hide {@link ImmutableSet#builderWithExpectedSize}
   * from consumers of {@code ImmutableSortedSet}.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Not supported by ImmutableSortedSet.
   */
  @DoNotCall("Use naturalOrder (which does not accept an expected size)")
  @Deprecated
  public static <E> ImmutableSortedSet.Builder<E> builderWithExpectedSize(int expectedSize) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a set that may contain a non-{@code Comparable}
   * element.</b> Proper calls will resolve to the version in {@code ImmutableSortedSet}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass a parameter of type {@code Comparable} to use {@link
   *     ImmutableSortedSet#of(Comparable)}.</b>
   */
  @DoNotCall("Pass a parameter of type Comparable")
  @Deprecated
  public static <E> ImmutableSortedSet<E> of(E e1) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a set that may contain a non-{@code Comparable}
   * element.</b> Proper calls will resolve to the version in {@code ImmutableSortedSet}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass the parameters of type {@code Comparable} to use {@link
   *     ImmutableSortedSet#of(Comparable, Comparable)}.</b>
   */
  @DoNotCall("Pass parameters of type Comparable")
  @Deprecated
  public static <E> ImmutableSortedSet<E> of(E e1, E e2) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a set that may contain a non-{@code Comparable}
   * element.</b> Proper calls will resolve to the version in {@code ImmutableSortedSet}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass the parameters of type {@code Comparable} to use {@link
   *     ImmutableSortedSet#of(Comparable, Comparable, Comparable)}.</b>
   */
  @DoNotCall("Pass parameters of type Comparable")
  @Deprecated
  public static <E> ImmutableSortedSet<E> of(E e1, E e2, E e3) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a set that may contain a non-{@code Comparable}
   * element.</b> Proper calls will resolve to the version in {@code ImmutableSortedSet}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass the parameters of type {@code Comparable} to use {@link
   *     ImmutableSortedSet#of(Comparable, Comparable, Comparable, Comparable)}. </b>
   */
  @DoNotCall("Pass parameters of type Comparable")
  @Deprecated
  public static <E> ImmutableSortedSet<E> of(E e1, E e2, E e3, E e4) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a set that may contain a non-{@code Comparable}
   * element.</b> Proper calls will resolve to the version in {@code ImmutableSortedSet}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass the parameters of type {@code Comparable} to use {@link
   *     ImmutableSortedSet#of( Comparable, Comparable, Comparable, Comparable, Comparable)}. </b>
   */
  @DoNotCall("Pass parameters of type Comparable")
  @Deprecated
  public static <E> ImmutableSortedSet<E> of(E e1, E e2, E e3, E e4, E e5) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a set that may contain a non-{@code Comparable}
   * element.</b> Proper calls will resolve to the version in {@code ImmutableSortedSet}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass the parameters of type {@code Comparable} to use {@link
   *     ImmutableSortedSet#of(Comparable, Comparable, Comparable, Comparable, Comparable,
   *     Comparable, Comparable...)}. </b>
   */
  @DoNotCall("Pass parameters of type Comparable")
  @Deprecated
  public static <E> ImmutableSortedSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... remaining) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a set that may contain non-{@code Comparable}
   * elements.</b> Proper calls will resolve to the version in {@code ImmutableSortedSet}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass parameters of type {@code Comparable} to use {@link
   *     ImmutableSortedSet#copyOf(Comparable[])}.</b>
   */
  @DoNotCall("Pass parameters of type Comparable")
  @Deprecated
  // The usage of "Z" here works around bugs in Javadoc (JDK-8318093) and JDiff.
  public static <Z> ImmutableSortedSet<Z> copyOf(Z[] elements) {
    throw new UnsupportedOperationException();
  }

  @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0xdecaf;
}
