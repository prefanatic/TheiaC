// Generated code from Butter Knife. Do not modify!
package com.example.theia;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class OpenCVActivity$$ViewBinder<T extends com.example.theia.OpenCVActivity> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131492973, "field 'cameraView'");
    target.cameraView = finder.castView(view, 2131492973, "field 'cameraView'");
  }

  @Override public void unbind(T target) {
    target.cameraView = null;
  }
}
