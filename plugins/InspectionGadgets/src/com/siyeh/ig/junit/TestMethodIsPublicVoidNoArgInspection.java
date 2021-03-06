/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.siyeh.ig.junit;

import com.intellij.codeInspection.util.SpecialAnnotationsUtil;
import com.intellij.psi.PsiMethod;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Bas Leijdekkers
 */
public class TestMethodIsPublicVoidNoArgInspection extends TestMethodIsPublicVoidNoArgInspectionBase {

  @Nullable
  @Override
  public JComponent createOptionsPanel() {
    return SpecialAnnotationsUtil.createSpecialAnnotationsListControl(
      ignorableAnnotations, InspectionGadgetsBundle.message("ignore.parameter.if.annotated.by"));
  }

  @Override
  protected InspectionGadgetsFix buildFix(Object... infos) {
    final PsiMethod method = (PsiMethod)infos[1];
    return new MakePublicStaticVoidFix(method, false);
  }
}
