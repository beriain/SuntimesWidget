/**
    Copyright (C) 2017 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/ 

package com.forrestguice.suntimeswidget.themes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.ColorChooser;
import com.forrestguice.suntimeswidget.settings.PaddingChooser;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.settings.WidgetThemes;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;

import static com.forrestguice.suntimeswidget.themes.SuntimesTheme.THEME_NAME;

public class WidgetThemeConfigActivity extends AppCompatActivity
{
    public static final int MIN_TITLE_SIZE = 8;
    public static final int MAX_TITLE_SIZE = 48;

    public static final String PARAM_MODE = "mode";

    public static final int ADD_THEME_REQUEST = 0;
    public static final int EDIT_THEME_REQUEST = 1;
    public static enum UIMode { ADD_THEME, EDIT_THEME }

    private static UIMode mode = UIMode.ADD_THEME;
    public UIMode getMode()
    {
        return mode;
    }

    private UIMode param_mode = null;
    private String param_themeName = null;

    private ActionBar actionBar;
    private EditText editDisplay;
    private TitleSizeChooser chooseTitleSize;
    private ThemeNameChooser chooseName;
    private PaddingChooser choosePadding;
    private ColorChooser chooseColorRise, chooseColorSet, chooseColorTitle, chooseColorText, chooseColorTime, chooseColorSuffix;
    private ArrayList<ColorChooser> colorChoosers;
    private Spinner spinBackground;
    //protected ThemeBackground[] backgrounds;

    private View previewBackground;
    private TextView previewTitle, previewNoon, previewRise, previewSet, previewNoonSuffix, previewRiseSuffix, previewSetSuffix;
    private TextView previewTimeDeltaPrefix, previewTimeDelta, previewTimeDeltaSuffix;

    private SuntimesUtils utils = new SuntimesUtils();

    public WidgetThemeConfigActivity()
    {
        super();
    }

    @Override
    public void onCreate(Bundle icicle)
    {
        setTheme(AppSettings.loadTheme(this));
        super.onCreate(icicle);
        initLocale();
        setResult(RESULT_CANCELED);
        setContentView(R.layout.layout_themeconfig);

        Intent intent = getIntent();
        param_mode = (UIMode)intent.getSerializableExtra(PARAM_MODE);
        param_themeName = intent.getStringExtra(THEME_NAME);

        mode = (param_mode == null) ? UIMode.ADD_THEME : param_mode;

        initViews(this);
        loadTheme(param_themeName);
        updatePreview();
    }

    private void initLocale()
    {
        AppSettings.initLocale(this);
        WidgetSettings.initDefaults(this);
        WidgetSettings.initDisplayStrings(this);
        ThemeBackground.initDisplayStrings(this);
        SuntimesUtils.initDisplayStrings(this);
    }

    protected void initViews( Context context )
    {
        Toolbar menuBar = (Toolbar) findViewById(R.id.app_menubar);
        setSupportActionBar(menuBar);
        actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initPreview(context);

        colorChoosers = new ArrayList<>();

        //backgrounds = new ThemeBackground[3];
        //backgrounds[0] = new ThemeBackground(R.drawable.bg_widget_dark, getString(R.string.configLabel_themeBackground_dark));
        //backgrounds[1] = new ThemeBackground(R.drawable.bg_widget, getString(R.string.configLabel_themeBackground_light));
        //backgrounds[2] = new ThemeBackground(android.R.color.transparent, getString(R.string.configLabel_themeBackground_trans));

        ArrayAdapter<ThemeBackground> spinBackground_adapter = new ArrayAdapter<>(this, R.layout.layout_listitem_oneline, ThemeBackground.values());
        spinBackground_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinBackground = (Spinner)findViewById(R.id.editSpin_background);
        spinBackground.setAdapter(spinBackground_adapter);
        spinBackground.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                updatePreview();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
                updatePreview();
            }
        });

        EditText editName = (EditText)findViewById(R.id.edit_themeName);
        chooseName = new ThemeNameChooser(editName);

        editDisplay = (EditText)findViewById(R.id.edit_themeDisplay);
        editDisplay.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View view, boolean hasFocus)
            {
                if (!hasFocus)
                {
                    if (validateThemeDisplayText(WidgetThemeConfigActivity.this, editDisplay, false))
                    {
                        updatePreview();
                    }
                }
            }
        });

        EditText editTitleSize = (EditText)findViewById(R.id.edit_titleSize);
        chooseTitleSize = new TitleSizeChooser(editTitleSize);

        EditText editPadding = (EditText)findViewById(R.id.edit_padding);
        choosePadding = new PaddingChooser(editPadding)
        {
            @Override
            protected void onPaddingChanged( int[] newPadding )
            {
                updatePreview();
            }
        };

        EditText editColorTitle = (EditText)findViewById(R.id.edit_titleColor);
        ImageButton buttonColorTitle = (ImageButton)findViewById(R.id.editButton_titleColor);
        chooseColorTitle = new ColorChooser(this, editColorTitle, buttonColorTitle, SuntimesTheme.THEME_TITLECOLOR)
        {
            @Override
            protected void onColorChanged( int newColor )
            {
                updatePreview();
            }
        };
        colorChoosers.add(chooseColorTitle);

        EditText editColorText = (EditText)findViewById(R.id.edit_textColor);
        ImageButton buttonColorText = (ImageButton)findViewById(R.id.editButton_textColor);
        chooseColorText = new ColorChooser(this, editColorText, buttonColorText, SuntimesTheme.THEME_TEXTCOLOR)
        {
            @Override
            protected void onColorChanged( int newColor )
            {
                updatePreview();
            }
        };
        colorChoosers.add(chooseColorText);

        EditText editColorRise = (EditText)findViewById(R.id.edit_sunriseColor);
        ImageButton buttonColorRise = (ImageButton)findViewById(R.id.editButton_sunriseColor);
        chooseColorRise = new ColorChooser(this, editColorRise, buttonColorRise, SuntimesTheme.THEME_SUNRISECOLOR)
        {
            @Override
            protected void onColorChanged( int newColor )
            {
                updatePreview();
            }
        };
        colorChoosers.add(chooseColorRise);

        EditText editColorSet = (EditText)findViewById(R.id.edit_sunsetColor);
        ImageButton buttonColorSet = (ImageButton)findViewById(R.id.editButton_sunsetColor);
        chooseColorSet = new ColorChooser(this, editColorSet, buttonColorSet, SuntimesTheme.THEME_SUNSETCOLOR)
        {
            @Override
            protected void onColorChanged( int newColor )
            {
                updatePreview();
            }
        };
        colorChoosers.add(chooseColorSet);

        EditText editColorTime = (EditText)findViewById(R.id.edit_timeColor);
        ImageButton buttonColorTime = (ImageButton)findViewById(R.id.editButton_timeColor);
        chooseColorTime = new ColorChooser(this, editColorTime, buttonColorTime, SuntimesTheme.THEME_TIMECOLOR)
        {
            @Override
            protected void onColorChanged( int newColor )
            {
                updatePreview();
            }
        };
        colorChoosers.add(chooseColorTime);

        EditText editColorSuffix = (EditText)findViewById(R.id.edit_suffixColor);
        ImageButton buttonColorSuffix = (ImageButton)findViewById(R.id.editButton_suffixColor);
        chooseColorSuffix = new ColorChooser(this, editColorSuffix, buttonColorSuffix, SuntimesTheme.THEME_TIMESUFFIXCOLOR)
        {
            @Override
            protected void onColorChanged( int newColor )
            {
                updatePreview();
            }
        };
        colorChoosers.add(chooseColorSuffix);

        for (ColorChooser chooser : colorChoosers)
        {
            chooser.setFragmentManager(getSupportFragmentManager());
        }

        switch (mode)
        {
            case EDIT_THEME:
                actionBar.setTitle(getString(R.string.configLabel_widgetThemeEdit));
                editName.setEnabled(false);
                break;

            case ADD_THEME:
            default:
                actionBar.setTitle(getString(R.string.configLabel_widgetThemeAdd));
                editName.setEnabled(true);
                break;

        }
    }

    protected void initPreview(Context context)
    {
        previewBackground = findViewById(R.id.widgetframe_inner);
        previewTitle = (TextView)findViewById(R.id.text_title);

        previewNoon = (TextView)findViewById(R.id.text_time_noon);
        previewNoonSuffix = (TextView)findViewById(R.id.text_time_noon_suffix);

        previewRise = (TextView)findViewById(R.id.text_time_sunrise);
        previewRiseSuffix = (TextView)findViewById(R.id.text_time_sunrise_suffix);

        previewSet = (TextView)findViewById(R.id.text_time_sunset);
        previewSetSuffix = (TextView)findViewById(R.id.text_time_sunset_suffix);

        previewTimeDelta = (TextView)findViewById(R.id.text_delta_day_value);
        previewTimeDeltaPrefix = (TextView)findViewById(R.id.text_delta_day_prefix);
        previewTimeDeltaSuffix = (TextView)findViewById(R.id.text_delta_day_suffix);
    }

    protected void updatePreview()
    {
        if (previewBackground != null)
        {
            ThemeBackground background = (ThemeBackground)spinBackground.getSelectedItem();
            if (background != null)
            {
                int[] padding = choosePadding.getPaddingPixels(this);
                previewBackground.setBackgroundResource(background.getResID());
                previewBackground.setPadding(padding[0], padding[1], padding[2], padding[3]);
            }
        }

        if (previewTitle != null)
        {
            String displayText = editDisplay.getText().toString().trim();
            String titleText = (displayText.isEmpty() ? chooseName.getThemeName() : displayText);
            previewTitle.setText(titleText);
            previewTitle.setVisibility(View.VISIBLE);
            previewTitle.setTextColor(chooseColorTitle.getColor());
            previewTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, chooseTitleSize.getTitleSize());
        }

        Calendar c0 = Calendar.getInstance();
        c0.set(Calendar.HOUR_OF_DAY, 12);
        c0.set(Calendar.MINUTE, 0);
        SuntimesUtils.TimeDisplayText noonText = utils.calendarTimeShortDisplayString(this, c0);

        if (previewNoon != null)
        {
            previewNoon.setText(noonText.getValue());
            previewNoon.setTextColor(chooseColorSet.getColor());
        }
        if (previewNoonSuffix != null)
        {
            previewNoonSuffix.setText(noonText.getSuffix());
            previewNoonSuffix.setTextColor(chooseColorSuffix.getColor());
        }

        Calendar c1 = Calendar.getInstance();
        c1.set(Calendar.HOUR_OF_DAY, 7);
        c1.set(Calendar.MINUTE, 0);
        SuntimesUtils.TimeDisplayText riseText = utils.calendarTimeShortDisplayString(this, c1);

        if (previewRise != null)
        {
            previewRise.setText(riseText.getValue());
            previewRise.setTextColor(chooseColorRise.getColor());
        }
        if (previewRiseSuffix != null)
        {
            previewRiseSuffix.setText(riseText.getSuffix());
            previewRiseSuffix.setTextColor(chooseColorSuffix.getColor());
        }

        Calendar c2 = Calendar.getInstance();
        c2.set(Calendar.HOUR_OF_DAY, 19);
        c2.set(Calendar.MINUTE, 0);
        SuntimesUtils.TimeDisplayText setText = utils.calendarTimeShortDisplayString(this, c2);

        if (previewSet != null)
        {
            previewSet.setText(setText.getValue());
            previewSet.setTextColor(chooseColorSet.getColor());
        }
        if (previewSetSuffix != null)
        {
            previewSetSuffix.setText(setText.getSuffix());
            previewSetSuffix.setTextColor(chooseColorSuffix.getColor());
        }

        if (previewTimeDelta != null)
        {
            int deltaSeconds = 60;
            previewTimeDelta.setText(utils.timeDeltaLongDisplayString(0, deltaSeconds * 1000).getValue());
            previewTimeDelta.setTextColor(chooseColorTime.getColor());
        }
        if (previewTimeDeltaPrefix != null)
        {
            previewTimeDeltaPrefix.setText(getString(R.string.delta_day_tomorrow));
            previewTimeDeltaPrefix.setTextColor(chooseColorText.getColor());
        }
        if (previewTimeDeltaSuffix != null)
        {
            previewTimeDeltaSuffix.setText(getString(R.string.delta_day_shorter));
            previewTimeDeltaSuffix.setTextColor(chooseColorText.getColor());
        }
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        for (ColorChooser chooser : colorChoosers)
        {
            chooser.onResume();
        }
    }

    @Override
    public void onSaveInstanceState( Bundle outState )
    {
        super.onSaveInstanceState(outState);
        outState.putString(SuntimesTheme.THEME_NAME, chooseName.getThemeName());
        outState.putString(SuntimesTheme.THEME_DISPLAYSTRING, editDisplay.getText().toString());

        ThemeBackground background = (ThemeBackground)spinBackground.getSelectedItem();
        if (background != null)
        {
            outState.putInt(SuntimesTheme.THEME_BACKGROUND, background.getResID());
        }

        outState.putInt(SuntimesTheme.THEME_TITLESIZE, chooseTitleSize.getTitleSize());
        outState.putInt(SuntimesTheme.THEME_TITLECOLOR, chooseColorTitle.getColor());
        outState.putInt(SuntimesTheme.THEME_TEXTCOLOR, chooseColorText.getColor());
        outState.putInt(SuntimesTheme.THEME_SUNRISECOLOR, chooseColorRise.getColor());
        outState.putInt(SuntimesTheme.THEME_SUNSETCOLOR, chooseColorSet.getColor());
        outState.putInt(SuntimesTheme.THEME_TIMECOLOR, chooseColorTime.getColor());
        outState.putInt(SuntimesTheme.THEME_TIMESUFFIXCOLOR, chooseColorSuffix.getColor());
        outState.putIntArray("padding", choosePadding.getPadding());
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedState)
    {
        super.onRestoreInstanceState(savedState);
        if (mode == UIMode.ADD_THEME)
        {
            chooseName.setThemeName(savedState.getString(SuntimesTheme.THEME_NAME, chooseName.getThemeName()));
        }
        editDisplay.setText(savedState.getString(SuntimesTheme.THEME_DISPLAYSTRING, editDisplay.getText().toString()));

        ThemeBackground background = (ThemeBackground)spinBackground.getSelectedItem();
        setSelectedBackground(savedState.getInt(SuntimesTheme.THEME_BACKGROUND, (background != null ? background.getResID() : DarkTheme.THEMEDEF_BACKGROUND_ID)));

        chooseTitleSize.setTitleSize(savedState.getInt(SuntimesTheme.THEME_TITLESIZE, this.chooseTitleSize.getTitleSize()));
        chooseColorTitle.setColor(savedState.getInt(SuntimesTheme.THEME_TITLECOLOR, chooseColorTitle.getColor()));
        chooseColorText.setColor(savedState.getInt(SuntimesTheme.THEME_TEXTCOLOR, chooseColorText.getColor()));
        chooseColorRise.setColor(savedState.getInt(SuntimesTheme.THEME_SUNRISECOLOR, chooseColorRise.getColor()));
        chooseColorSet.setColor(savedState.getInt(SuntimesTheme.THEME_SUNSETCOLOR, chooseColorSet.getColor()));
        chooseColorTime.setColor(savedState.getInt(SuntimesTheme.THEME_TIMECOLOR, chooseColorTime.getColor()));
        chooseColorSuffix.setColor(savedState.getInt(SuntimesTheme.THEME_TIMESUFFIXCOLOR, chooseColorSuffix.getColor()));
        choosePadding.setPadding(savedState.getIntArray("padding"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.themeconfig, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.saveTheme:
                onSaveClicked();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSaveClicked()
    {
        if (validateInput())
        {
            SuntimesTheme theme = saveTheme();
            if (theme != null)
            {
                Intent intent = new Intent();
                intent.putExtra(SuntimesTheme.THEME_NAME, theme.themeName());
                WidgetThemes.addValue(WidgetThemeConfigActivity.this, theme.themeDescriptor());
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        }
    }

    /**
     * loads values from an existing theme into ui fields,
     */
    protected void loadTheme( String themeName )
    {
        if (themeName != null)
        {
            chooseName.setThemeName( (mode == UIMode.ADD_THEME) ? generateThemeName(themeName) : themeName );

        } else if (mode == UIMode.ADD_THEME) {
            chooseName.setThemeName( suggestThemeName() );
            editDisplay.requestFocus();
        }

        try {
            String themeID = (themeName == null ? WidgetSettings.PREF_DEF_APPEARANCE_THEME : themeName);
            SuntimesTheme theme = WidgetThemes.loadTheme(this, themeID);
            if (themeName != null)
            {
                editDisplay.setText((mode == UIMode.ADD_THEME) ? generateThemeDisplayString(theme.themeDisplayString()) : theme.themeDisplayString());
            }
            chooseTitleSize.setTitleSize((int)theme.getTitleSizeSp());
            chooseColorTitle.setColor(theme.getTitleColor());
            chooseColorText.setColor(theme.getTextColor());
            chooseColorRise.setColor(theme.getSunriseTextColor());
            chooseColorSet.setColor(theme.getSunsetTextColor());
            chooseColorTime.setColor(theme.getTimeColor());
            chooseColorSuffix.setColor(theme.getTimeSuffixColor());
            choosePadding.setPadding(theme.getPadding());
            setSelectedBackground(theme.getBackgroundId());

        } catch (InvalidParameterException e) {
            Log.e("loadTheme", "unable to load theme: " + e);
        }
    }

    private void setSelectedBackground(int resId)
    {
        int backgroundPos = ThemeBackground.ordinal(resId);
        spinBackground.setSelection( backgroundPos < 0 ? 0 : backgroundPos );
    }

    /**
     *
     */
    protected SuntimesTheme saveTheme()
    {
        SuntimesTheme theme = new SuntimesTheme()
        {
            private SuntimesTheme init()
            {
                this.themeName = chooseName.getThemeName();
                this.themeDisplayString = editDisplay.getText().toString();
                this.themeTitleSize = chooseTitleSize.getTitleSize();
                this.themeTitleColor = chooseColorTitle.getColor();
                this.themeTextColor = chooseColorText.getColor();
                this.themeTimeColor = chooseColorTime.getColor();
                this.themeTimeSuffixColor = chooseColorSuffix.getColor();
                this.themeSunriseTextColor = chooseColorRise.getColor();
                this.themeSunsetTextColor = chooseColorSet.getColor();
                this.themePadding = choosePadding.getPadding();
                ThemeBackground backgroundItem = (ThemeBackground)spinBackground.getSelectedItem();
                if (backgroundItem != null)
                {
                    this.themeBackground = backgroundItem.getResID();
                }
                return this;
            }
        }.init();

        SharedPreferences themePref = getSharedPreferences(WidgetThemes.PREFS_THEMES, Context.MODE_PRIVATE);
        theme.saveTheme(themePref);
        return theme;
    }

    protected String suggestThemeName()
    {
        int i = 1;
        String generatedName;
        do {
            generatedName = getString(R.string.addtheme_custname, i+"");
            i++;
        } while (WidgetThemes.valueOf(generatedName) != null);
        return generatedName;
    }

    /**
     * @param suggestedName the desired themeName (might not be unique/available)
     * @return a unique themeName
     */
    protected String generateThemeName( String suggestedName )
    {
        int i = 1;
        String generatedName = suggestedName;
        while (WidgetThemes.valueOf(generatedName) != null)
        {
            generatedName = getString(R.string.addtheme_copyname, suggestedName, i+"");
            i++;
        }
        return generatedName;
    }

    protected String generateThemeDisplayString( String suggestedName )
    {
        return getString(R.string.addtheme_copydisplay, suggestedName);
    }

    /**
     * @return true fields are valid, false one or more fields is invalid
     */
    protected boolean validateInput()
    {
        boolean isValid = validateTitleSize(chooseTitleSize.getField());
        isValid = isValid && validateThemeDisplayText(editDisplay);
        isValid = isValid && validateThemeID(chooseName.getField());
        return isValid;
    }

    protected boolean validateThemeID( EditText editName )
    {
        return validateThemeID(this, editName, true);
    }
    protected static boolean validateThemeID(Context context, EditText editName, boolean grabFocus )
    {
        boolean isValid = true;
        editName.setError(null);

        String themeID = editName.getText().toString().trim();
        if (themeID.isEmpty())
        {
            isValid = false;       // themeName is required
            editName.setError(context.getString(R.string.edittheme_error_themeName_empty));
            if (grabFocus)
                editName.requestFocus();
        }
        if (mode == UIMode.ADD_THEME && WidgetThemes.valueOf(editName.getText().toString()) != null)
        {
            isValid = false;       // themeName is already taken
            editName.setError(context.getString(R.string.edittheme_error_themeName_unique));
            if (grabFocus)
                editName.requestFocus();
        }
        return isValid;
    }

    protected boolean validateThemeDisplayText( EditText editDisplay )
    {
        return validateThemeDisplayText(this, editDisplay, true);
    }
    protected static boolean validateThemeDisplayText(Context context, EditText editDisplay, boolean grabFocus )
    {
        boolean isValid = true;
        editDisplay.setError(null);

        if (editDisplay.getText().toString().trim().isEmpty())
        {
            isValid = false;     // display text is empty
            editDisplay.setError(context.getString(R.string.edittheme_error_displaytext));
            if (grabFocus)
                editDisplay.requestFocus();
        }
        return isValid;
    }

    protected boolean validateTitleSize( EditText editTitleSize )
    {
        return validateTitleSize(this, editTitleSize, true);
    }
    protected static boolean validateTitleSize(Context context, EditText editTitleSize, boolean grabFocus )
    {
        boolean isValid = true;
        editTitleSize.setError(null);

        try {
            int titleSize = Integer.parseInt(editTitleSize.getText().toString());
            if (titleSize < MIN_TITLE_SIZE)
            {
                isValid = false;       // title too small
                editTitleSize.setError(context.getString(R.string.edittheme_error_titlesize_min, MIN_TITLE_SIZE+""));
                if (grabFocus)
                    editTitleSize.requestFocus();
            }

            if (titleSize > MAX_TITLE_SIZE)
            {
                isValid = false;       // title too large
                editTitleSize.setError(context.getString(R.string.edittheme_error_titlesize_max, MAX_TITLE_SIZE+""));
                if (grabFocus)
                    editTitleSize.requestFocus();
            }

        } catch (NumberFormatException e) {
            isValid = false;          // title NaN (too small)
            editTitleSize.setError(context.getString(R.string.edittheme_error_titlesize_min, MIN_TITLE_SIZE+""));
            if (grabFocus)
                editTitleSize.requestFocus();
        }
        return isValid;
    }

    /**
     * TitleSizeChooser
     */
    private class TitleSizeChooser implements TextWatcher, View.OnFocusChangeListener
    {
        private int titleSize;
        private EditText edit;

        public TitleSizeChooser( EditText editField )
        {
            edit = editField;
            edit.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            edit.addTextChangedListener(this);
            edit.setOnFocusChangeListener(this);
        }

        public EditText getField()
        {
            return edit;
        }

        public int getTitleSize()
        {
            return titleSize;
        }

        public void setTitleSize( int spValue )
        {
            titleSize = spValue;
            updateViews();
        }

        public void updateViews()
        {
            edit.setText(""+titleSize);
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable)
        {
            String spValue = editable.toString();
            try {
                titleSize = Integer.parseInt(spValue);
            } catch (NumberFormatException e) {
                Log.w("setTitleSize", "Invalid size! " + spValue + " ignoring...");
                updateViews();
            }
        }

        @Override
        public void onFocusChange(View view, boolean hasFocus)
        {
            if (!hasFocus)
            {
                afterTextChanged(edit.getText());
                if (validateTitleSize(WidgetThemeConfigActivity.this, edit, false))
                {
                    updatePreview();
                }
            }
        }
    }

    /**
     * ThemeNameChooser
     */
    private class ThemeNameChooser implements TextWatcher, View.OnFocusChangeListener
    {
        private EditText edit;
        private String themeName;

        public ThemeNameChooser( EditText editField )
        {
            edit = editField;
            edit.setRawInputType(InputType.TYPE_CLASS_TEXT);
            edit.addTextChangedListener(this);
            edit.setOnFocusChangeListener(this);
        }

        public EditText getField()
        {
            return edit;
        }

        public String getThemeName()
        {
            return themeName;
        }

        public void setThemeName( String themeName )
        {
            this.themeName = themeName;
            updateViews();
        }

        public void updateViews()
        {
            edit.setText(themeName);
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable)
        {
            themeName = editable.toString();
            previewTitle.setText(editable);
        }

        @Override
        public void onFocusChange(View view, boolean hasFocus)
        {
            if (!hasFocus)
            {
                afterTextChanged(edit.getText());
                if (validateThemeID(WidgetThemeConfigActivity.this, edit, false))
                {
                    updateViews();
                }
            }
        }
    }

}
