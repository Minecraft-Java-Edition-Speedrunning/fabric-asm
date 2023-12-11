/*
 * Copyright 2019 Chocohead
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.chocohead.mm;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Retention(CLASS)
@Target({METHOD, CONSTRUCTOR})
@Repeatable(CorrectedMethods.class)
public @interface CorrectedMethod {
    String from();

    String to();
}