/*
 * Copyright (C) 2009 The Guava Authors
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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

/**
 * GWT emulated version of {@link RegularImmutableList}.
 *
 * @author Hayward Chan
 */
class RegularImmutableList<E> extends ForwardingImmutableList<E> {
  static final ImmutableList<Object> EMPTY = new RegularImmutableList<Object>(emptyList());

  private final List<E> delegate;

  RegularImmutableList(List<E> delegate) {
    // TODO(cpovirk): avoid redundant unmodifiableList wrapping
    this.delegate = unmodifiableList(delegate);
  }

  @Override
  List<E> delegateList() {
    return delegate;
  }
}
