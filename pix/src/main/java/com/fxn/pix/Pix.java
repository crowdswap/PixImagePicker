package com.fxn.pix;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fxn.adapters.InstantImageAdapter;
import com.fxn.adapters.MainImageAdapter;
import com.fxn.interfaces.OnSelectionListener;
import com.fxn.interfaces.WorkFinish;
import com.fxn.modals.Img;
import com.fxn.utility.Constants;
import com.fxn.utility.HeaderItemDecoration;
import com.fxn.utility.ImageFetcher;
import com.fxn.utility.PermUtil;
import com.fxn.utility.Utility;
import com.fxn.utility.ui.FastScrollStateChangeListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import io.fotoapparat.Fotoapparat;
import io.fotoapparat.configuration.CameraConfiguration;
import io.fotoapparat.error.CameraErrorListener;
import io.fotoapparat.exception.camera.CameraException;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.result.BitmapPhoto;
import io.fotoapparat.selector.FlashSelectorsKt;
import io.fotoapparat.selector.LensPositionSelectorsKt;
import io.fotoapparat.view.CameraView;
import io.fotoapparat.view.FocusView;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

public class Pix extends AppCompatActivity {

  private static final int sBubbleAnimDuration = 1000;
  private static final int sScrollbarHideDelay = 1000;
  private static final String OPTIONS = "options";
  private static final int sTrackSnapRange = 5;
  public static String IMAGE_RESULTS = "image_results";
  public static float TOPBAR_HEIGHT;
  CameraView cameraView;
  FocusView focusView;
  boolean camAvail = true;
  Handler handler2 = new Handler();
  private int BottomBarHeight = 0;
  private int colorPrimaryDark;
  private Fotoapparat fotoapparat;
  Runnable runnable = new Runnable() {
    @Override public void run() {
      // Log.e("start","foto apparat-----------------------------------------------------------------");
      if (camAvail) {
        fotoapparat.start();
      }
    }
  };
  private float zoom = 0.0f;
  private float dist = 0.0f;
  private Handler handler = new Handler();
  private InstantImageAdapter initaliseadapter;
  private ViewPropertyAnimator mScrollbarAnimator;
  private ViewPropertyAnimator mBubbleAnimator;
  private Set<Img> selectionList = new HashSet<>();
  private MainImageAdapter mainImageAdapter;
  private boolean mHideScrollbar = true;
  private boolean LongSelection = false;
  private Options options = null;

  private FrameLayout flash;
  private ImageView front;
  private int flashDrawable;
  private View.OnTouchListener onCameraTouchListner = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      if (event.getPointerCount() > 1) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
          case MotionEvent.ACTION_POINTER_DOWN:
            dist = Utility.getFingerSpacing(event);
            break;
          case MotionEvent.ACTION_MOVE:
            float maxZoom = 1f;

            float newDist = Utility.getFingerSpacing(event);
            if (newDist > dist) {
              //zoom in
              if (zoom < maxZoom) {
                zoom = zoom + 0.01f;
              }
            } else if ((newDist < dist) && (zoom > 0)) {
              //zoom out
              zoom = zoom - 0.01f;
            }
            dist = newDist;
            fotoapparat.setZoom(zoom);
            break;
          default:
            break;
        }
      }
      return false;
    }
  };

  public static void start(final Fragment context, final Options options) {
    PermUtil.checkForCamaraWritePermissions(context, new WorkFinish() {
      @Override
      public void onWorkFinish(Boolean check) {
        Intent i = new Intent(context.getActivity(), Pix.class);
        i.putExtra(OPTIONS, options);
        context.startActivityForResult(i, options.getRequestCode());
      }
    });
  }

  public static void start(Fragment context, int requestCode) {
    start(context, Options.init().setRequestCode(requestCode).setCount(1));
  }

  public static void start(final FragmentActivity context, final Options options) {
    PermUtil.checkForCamaraWritePermissions(context, new WorkFinish() {
      @Override
      public void onWorkFinish(Boolean check) {
        Intent i = new Intent(context, Pix.class);
        i.putExtra(OPTIONS, options);
        context.startActivityForResult(i, options.getRequestCode());
      }
    });
  }

  public static void start(final FragmentActivity context, int requestCode) {
    start(context, Options.init().setRequestCode(requestCode).setCount(1));
  }

  public void returnObjects() {
    ArrayList<String> list = new ArrayList<>();
    for (Img i : selectionList) {
      list.add(i.getUrl());
      // Log.e("Pix images", "img " + i.getUrl());
    }
    Intent resultIntent = new Intent();
    resultIntent.putStringArrayListExtra(IMAGE_RESULTS, list);
    setResult(Activity.RESULT_OK, resultIntent);
    finish();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Utility.setupStatusBarHidden(this);
    Utility.hideStatusBar(this);
    setContentView(R.layout.activity_main_lib);
    initialize();
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    handler2.postDelayed(runnable, 0);
  }

  @Override
  protected void onResume() {
    super.onResume();
    handler2.postDelayed(runnable, 0);
  }

  @Override
  protected void onPause() {
    fotoapparat.stop();
    super.onPause();
  }

  private void initialize() {
    Utility.getScreenSize(this);
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }
    try {
      options = (Options) getIntent().getSerializableExtra(OPTIONS);
    } catch (Exception e) {
      e.printStackTrace();
    }
    setRequestedOrientation(options.getScreenOrientation());
    colorPrimaryDark =
        ResourcesCompat.getColor(getResources(), R.color.colorPrimaryPix, getTheme());
    cameraView = findViewById(R.id.camera_view);
    focusView = findViewById(R.id.focusView);
    try {
      fotoapparat = Fotoapparat
          .with(this)
          .into(cameraView)
          .focusView(focusView)
          .previewScaleType(ScaleType.CenterCrop)
          .cameraErrorCallback(new CameraErrorListener() {
            @Override
            public void onError(@NotNull CameraException e) {
              Toast.makeText(Pix.this, e.toString(), Toast.LENGTH_LONG).show();
            }
          })
          .build();
    } catch (Exception e) {
      e.printStackTrace();
    }
    zoom = 0.0f;
    focusView.setOnTouchListener(onCameraTouchListner);
    handler2.postDelayed(runnable, 0);
    fotoapparat.updateConfiguration(
        CameraConfiguration.builder().flash(FlashSelectorsKt.autoRedEye()).build());
    flash = findViewById(R.id.flash);
    front = findViewById(R.id.front);
    TOPBAR_HEIGHT = Utility.convertDpToPixel(56, Pix.this);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
    initaliseadapter = new InstantImageAdapter(this);
    FrameLayout mainFrameLayout = findViewById(R.id.mainFrameLayout);
    BottomBarHeight = Utility.getSoftButtonsBarSizePort(this);
    FrameLayout.LayoutParams lp =
        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT);
    lp.setMargins(0, 0, 0, BottomBarHeight);
    mainFrameLayout.setLayoutParams(lp);
    mainImageAdapter = new MainImageAdapter(this);
    GridLayoutManager mLayoutManager = new GridLayoutManager(this, MainImageAdapter.SPAN_COUNT);
    mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
      @Override
      public int getSpanSize(int position) {
        if (mainImageAdapter.getItemViewType(position) == MainImageAdapter.HEADER) {
          return MainImageAdapter.SPAN_COUNT;
        }
        return 1;
      }
    });
    final CameraConfiguration cameraConfiguration = new CameraConfiguration();
    if (options.isFrontfacing()) {
      fotoapparat.switchTo(LensPositionSelectorsKt.front(), cameraConfiguration);
    } else {
      fotoapparat.switchTo(LensPositionSelectorsKt.back(), cameraConfiguration);
    }
    onClickMethods();

    flashDrawable = R.drawable.ic_flash_off_black_24dp;

    if ((options.getPreSelectedUrls().size()) > options.getCount()) {
      int large = options.getPreSelectedUrls().size() - 1;
      int small = options.getCount();
      for (int i = large; i > (small - 1); i--) {
        options.getPreSelectedUrls().remove(i);
      }
    }
    updateImages();
  }

  private void onClickMethods() {
    findViewById(R.id.clickme).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (selectionList.size() >= options.getCount()) {
          Toast.makeText(Pix.this,
              String.format(getResources().getString(R.string.cannot_click_image_pix),
                  "" + options.getCount()), Toast.LENGTH_LONG).show();
          return;
        }

        final ObjectAnimator oj = ObjectAnimator.ofFloat(cameraView, "alpha", 1f, 0f, 0f, 1f);
        oj.setStartDelay(200l);
        oj.setDuration(900l);
        oj.start();
        Log.e("click time", "--------------------------------");
        fotoapparat.takePicture().toBitmap().transform(new Function1<BitmapPhoto, Bitmap>() {
          @Override
          public Bitmap invoke(BitmapPhoto bitmapPhoto) {
            Log.e("click time", "--------------------------------1");
            return Utility.rotate(bitmapPhoto.bitmap, bitmapPhoto.rotationDegrees);
          }
        }).whenAvailable(new Function1<Bitmap, Unit>() {
          @Override
          public Unit invoke(Bitmap bitmap) {
            if (bitmap != null) {
              synchronized (bitmap) {
                Utility.vibe(Pix.this, 50);
                File photo =
                    Utility.writeImage(bitmap, options.getPath(), options.getImageQuality(),
                        options.getWidth(), options.getHeight());
                Img img = new Img("", "", photo.getAbsolutePath(), "");
                selectionList.add(img);
                Utility.scanPhoto(Pix.this, photo);
                Log.e("click time", "--------------------------------2");
                returnObjects();
              }
            }
            return null;
          }
        });
      }
    });

    final ImageView iv = (ImageView) flash.getChildAt(0);
    flash.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        final int height = flash.getHeight();
        iv.animate()
            .translationY(height)
            .setDuration(100)
            .setListener(new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                iv.setTranslationY(-(height / 2));
                if (flashDrawable == R.drawable.ic_flash_auto_black_24dp) {
                  flashDrawable = R.drawable.ic_flash_off_black_24dp;
                  iv.setImageResource(flashDrawable);
                  fotoapparat.updateConfiguration(
                      CameraConfiguration.builder().flash(FlashSelectorsKt.off()).build());
                } else if (flashDrawable == R.drawable.ic_flash_off_black_24dp) {
                  flashDrawable = R.drawable.ic_flash_on_black_24dp;
                  iv.setImageResource(flashDrawable);
                  fotoapparat.updateConfiguration(
                      CameraConfiguration.builder().flash(FlashSelectorsKt.on()).build());
                } else {
                  flashDrawable = R.drawable.ic_flash_auto_black_24dp;
                  iv.setImageResource(flashDrawable);
                  fotoapparat.updateConfiguration(
                      CameraConfiguration.builder().flash(FlashSelectorsKt.autoRedEye()).build());
                }
                // fotoapparat.focus();
                iv.animate().translationY(0).setDuration(50).setListener(null).start();
              }
            })
            .start();
      }
    });

    front.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        final ObjectAnimator oa1 = ObjectAnimator.ofFloat(front, "scaleX", 1f, 0f).setDuration(150);
        final ObjectAnimator oa2 = ObjectAnimator.ofFloat(front, "scaleX", 0f, 1f).setDuration(150);
        oa1.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            front.setImageResource(R.drawable.ic_photo_camera);
            oa2.start();
          }
        });
        oa1.start();
        if (options.isFrontfacing()) {
          options.setFrontfacing(false);
          final CameraConfiguration cameraConfiguration = new CameraConfiguration();
          fotoapparat.switchTo(LensPositionSelectorsKt.back(), cameraConfiguration);
        } else {
          final CameraConfiguration cameraConfiguration = new CameraConfiguration();
          options.setFrontfacing(true);
          fotoapparat.switchTo(LensPositionSelectorsKt.front(), cameraConfiguration);
        }
      }
    });
  }

  private void updateImages() {
    mainImageAdapter.clearList();
    Cursor cursor = Utility.getCursor(Pix.this);
    if (cursor == null) {
      return;
    }
    ArrayList<Img> INSTANTLIST = new ArrayList<>();
    String header = "";
    int limit = 100;
    if (cursor.getCount() < limit) {
      limit = cursor.getCount() - 1;
    }
    int date = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
    int data = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
    int contentUrl = cursor.getColumnIndex(MediaStore.Images.Media._ID);
    Calendar calendar;
    int pos = 0;
    for (int i = 0; i < limit; i++) {
      cursor.moveToNext();
      Uri path = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          "" + cursor.getInt(contentUrl));
      calendar = Calendar.getInstance();
      calendar.setTimeInMillis(cursor.getLong(date));
      String dateDifference = Utility.getDateDifference(Pix.this, calendar);
      if (!header.equalsIgnoreCase("" + dateDifference)) {
        header = "" + dateDifference;
        pos += 1;
        INSTANTLIST.add(new Img("" + dateDifference, "", "", ""));
      }
      Img img = new Img("" + header, "" + path, cursor.getString(data), "" + pos);
      img.setPosition(pos);
      if (options.getPreSelectedUrls().contains(img.getUrl())) {
        img.setSelected(true);
        selectionList.add(img);
      }
      pos += 1;
      INSTANTLIST.add(img);
    }
    if (selectionList.size() > 0) {
      LongSelection = true;
      Animation anim = new ScaleAnimation(
          0f, 1f, // Start and end values for the X axis scaling
          0f, 1f, // Start and end values for the Y axis scaling
          Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
          Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
      anim.setFillAfter(true); // Needed to keep the result of the animation
      anim.setDuration(300);
    }
    mainImageAdapter.addImageList(INSTANTLIST);
    initaliseadapter.addImageList(INSTANTLIST);
  }

  @Override
  public void onBackPressed() {
    finish();
    super.onBackPressed();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    handler2.removeCallbacks(runnable);
  }
}
