package im.adamant.android.helpers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

public class DrawableColorHelper {
    public static void changeColorForDrawable(Context context, TextView textView, int color, PorterDuff.Mode mode) {
        Drawable[] compoundDrawablesRelative = textView.getCompoundDrawablesRelative();
        Drawable[] newDrawablesRelative = changeColorForDrawable(context, color, mode, compoundDrawablesRelative);
        textView.setCompoundDrawablesRelative(newDrawablesRelative[0], newDrawablesRelative[1], newDrawablesRelative[2], newDrawablesRelative[3]);
    }

    private static Drawable[] changeColorForDrawable(
            Context context,
            int color,
            PorterDuff.Mode mode,
            Drawable[] drawables
    ) {
        int length = (drawables == null) ? 0 : drawables.length;
        Drawable[] newDrawables = new Drawable[length];

        if (length > 0){
            for (int position = 0; position < drawables.length; position++) {
                Drawable drawable = drawables[position];

                if (drawable != null) {
                    drawable = drawable.mutate();
                    drawable.setColorFilter(
                            new PorterDuffColorFilter(
                                    ContextCompat.getColor(context, color),
                                    mode
                            )
                    );
                    newDrawables[position] = drawable;
                }

            }
        }

        return newDrawables;
    }
}
