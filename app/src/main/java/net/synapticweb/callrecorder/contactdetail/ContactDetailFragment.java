package net.synapticweb.callrecorder.contactdetail;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.content.res.Configuration;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.Bundle;

import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;

import net.synapticweb.callrecorder.AppLibrary;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.TemplateActivity;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.Recording;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class ContactDetailFragment extends Fragment implements ContactDetailContract.View{
    private ContactDetailPresenter presenter;
    private RecordingAdapter adapter;
    private TextView typePhoneView, phoneNumberView, recordingStatusView;
    private ImageView contactPhotoView;
    private RecyclerView recordingsRecycler;
    private RelativeLayout detailView;
    private Contact contact;
    private boolean selectMode = false;
    private List<Integer> selectedItems = new ArrayList<>();
    private TemplateActivity parentActivity;
    private static final String ARG_CONTACT = "arg_contact";
    private static final String SELECT_MODE_KEY = "select_mode_key";
    private static final String SELECTED_ITEMS_KEY = "selected_items_key";
    private static final String TAG = "CallRecorder";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        parentActivity = (TemplateActivity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        parentActivity = null;
    }

    @Override
    public RecyclerView getRecordingsRecycler() {
        return recordingsRecycler;
    }

    @Override
    public RecordingAdapter getRecordingsAdapter() {
        return adapter;
    }

    @Override
    public List<Integer> getSelectedItems() {
        return selectedItems;
    }

    @Override
    public void setActionBarTitleIfActivityDetail() {
        TextView title = parentActivity.findViewById(R.id.actionbar_title);
        title.setText(contact.getContactName());
    }

    @Override
    public TemplateActivity getParentActivity() {
        return parentActivity;
    }

    @Override
    public List<Recording> getSelectedRecordings() {
        List<Recording> list = new ArrayList<>();
        for(int adapterPosition : selectedItems)
            list.add(adapter.getItem(adapterPosition));
        return list;
    }

    @Override
    public void setSelectMode(boolean isSelectModeOn) {
        this.selectMode = isSelectModeOn;
    }

    @Override
    public boolean isEmptySelectedItems() {
        return selectedItems.isEmpty();
    }

    @Override
    public boolean isSelectModeOn() {
        return selectMode;
    }

    @Override
    public void addToSelectedItems(int adapterPosition) {
        selectedItems.add(adapterPosition);
    }

    @Override
    public boolean removeIfPresentInSelectedItems(int adapterPosition) {
        if (selectedItems.contains(adapterPosition)) {
            selectedItems.remove((Integer) adapterPosition); //fără casting îl interpretează ca poziție
            //în selectedItems
            return true;
        }
        else
            return false;
    }

    @Override
    public boolean isSinglePaneLayout() {
        return (parentActivity != null &&
                parentActivity.findViewById(R.id.contacts_list_fragment_container) == null);
    }

    @Override
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public void onResume(){
        super.onResume();
        presenter.loadRecordings(contact);
    }

    private void toggleView(final View view, final boolean selectModeOn, Float alpha) {
        if(alpha == null) {
            view.animate()
                    .alpha(view.getAlpha() == 0.0f ? 1.0f : 0.0f)
                    .setDuration(250)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                        }
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            if (selectModeOn)
                                view.setVisibility(selectMode ? View.VISIBLE : View.GONE);
                            else
                                view.setVisibility(selectMode ? View.GONE : View.VISIBLE);
                        }
                        @Override
                        public void onAnimationCancel(Animator animator) {
                        }
                        @Override
                        public void onAnimationRepeat(Animator animator) {
                        }
                    });
        }
        else {
            view.setAlpha(alpha);
            if (selectModeOn)
                view.setVisibility(selectMode ? View.VISIBLE : View.GONE);
            else
                view.setVisibility(selectMode ? View.GONE : View.VISIBLE);
        }
    }

    //Această funcție este apelată în 2 situații: 1: Cînd se intră/iese din selectMode. În acest caz butoanele apar
    //și dispar cu un efect de fadein/fadeout. Butoanele care nu sunt vizibile cu selectMode=off au setat în layout
    //alpha=0. Celelalte nu au alpha setat, deci este implicit 1. Efectul constă în tranziția timp de 250ms a
    //proprietății alpha de la 0 la 1 și de la 1 la 0. La sfîrșitul tranziției se modifică vizibilitatea.
    //2: la construcția fragmentului, adică după încărcarea unui contact sau la rotirea ecranului. Mai înainte
    //funcția era apelată la fiecare onResume, dar asta a dus la buguri inutile și am renunțat.
    //animateAlpha este true în cazul 1 și false în cazul 2.
    //În cazul 1 nu ne interesează alte valori ale alpha decît cele pe care le are view-ul animat - tot ce
    //facem este să inversăm această valoare, deci al treilea aparametru al toggleView este null.
    //în cazul 2 avem 2 situații: selectMode=on, selectMode=off. Cînd selectMode=off setăm alpha la aceleași
    //valori ca în layout. Cînd selectMode=on inversăm aceste valori.
    @Override
    public void toggleSelectModeActionBar(boolean animateAplha) {
        ImageButton navigateBackBtn = parentActivity.findViewById(R.id.navigate_back);
        final ImageButton closeBtn = parentActivity.findViewById(R.id.close_select_mode);
        final ImageButton editBtn = parentActivity.findViewById(R.id.edit_contact);
        ImageButton callBtn = parentActivity.findViewById(R.id.call_contact);
        ImageButton exportBtn = parentActivity.findViewById(R.id.actionbar_select_export);
        ImageButton deleteBtn = parentActivity.findViewById(R.id.actionbar_select_delete);
        ImageButton selectAllBtn = parentActivity.findViewById(R.id.actionbar_select_all);
        ImageButton infoBtn = parentActivity.findViewById(R.id.actionbar_info);
        ImageButton menuRightBtn = parentActivity.findViewById(R.id.phone_number_detail_menu);

        if(isSinglePaneLayout())
            toggleView(navigateBackBtn, false, animateAplha ? null : selectMode ? 0f : 1f);

        toggleView(closeBtn, true, animateAplha ? null : selectMode ? 1f : 0f);
        if(!contact.isPrivateNumber()) {
            toggleView(editBtn, false, animateAplha ? null : selectMode ? 0f : 1f);
            toggleView(callBtn, false, animateAplha  ? null : selectMode ? 0f : 1f);
        }

        toggleView(exportBtn, true, animateAplha ? null : selectMode ? 1f : 0f);
        toggleView(deleteBtn, true, animateAplha  ? null  : selectMode ? 1f : 0f);
        toggleView(selectAllBtn, true, animateAplha ? null : selectMode ? 1f : 0f);
        toggleView(infoBtn, true, animateAplha ? null : selectMode ? 1f : 0f);
        toggleView(menuRightBtn, false, animateAplha ? null : selectMode ? 0f : 1f);

        if(!isSinglePaneLayout()) {
            Button hamburger = parentActivity.findViewById(R.id.hamburger);
            toggleView(hamburger, false, animateAplha ? null : selectMode ? 0f : 1f);
        }

    }

    @Override
    public void clearSelectedMode() {
        selectMode = false;
        toggleSelectModeActionBar(true);
        for(int i = 0; i < adapter.getItemCount(); ++i) {
            //https://stackoverflow.com/questions/33784369/recyclerview-get-view-at-particular-position
            View recordingSlot = recordingsRecycler.getLayoutManager().findViewByPosition(i);
            if (recordingSlot != null) { //este posibil ca recordingul să fi fost șters sau să nu fie în prezent
                //pe ecran.
                toggleSelectModeRecording(recordingSlot, true);
                deselectRecording(recordingSlot);
            }
            adapter.notifyItemChanged(i);
        }
        selectedItems.clear();
    }

    private void modifyMargins(View recording, float interpolatedTime) {
        CheckBox checkBox = recording.findViewById(R.id.recording_checkbox);
        Resources res = getContext().getResources();
        checkBox.setVisibility((selectMode ? View.VISIBLE : View.GONE));
        RelativeLayout.LayoutParams lpCheckBox = (RelativeLayout.LayoutParams) checkBox.getLayoutParams();
        lpCheckBox.setMarginStart(selectMode ?
                (int) (res.getDimension(R.dimen.recording_checkbox_visible_start_margin) * interpolatedTime) :
                (int) (res.getDimension(R.dimen.recording_checkbox_gone_start_margin) * interpolatedTime));
        checkBox.setLayoutParams(lpCheckBox);

        ImageView recordingAdorn = recording.findViewById(R.id.recording_adorn);
        RelativeLayout.LayoutParams lpRecAdorn = (RelativeLayout.LayoutParams) recordingAdorn.getLayoutParams();
        lpRecAdorn.setMarginStart(selectMode ?
                (int) (res.getDimension(R.dimen.recording_adorn_selected_margin_start) * interpolatedTime) :
                (int) (res.getDimension(R.dimen.recording_adorn_unselected_margin_start) * interpolatedTime));
        recordingAdorn.setLayoutParams(lpRecAdorn);

        TextView title = recording.findViewById(R.id.recording_title);
        RelativeLayout.LayoutParams lpTitle = (RelativeLayout.LayoutParams) title.getLayoutParams();
        lpTitle.setMarginStart(selectMode ?
                (int) (res.getDimension(R.dimen.recording_title_selected_margin_start) * interpolatedTime) :
                (int) (res.getDimension(R.dimen.recording_title_unselected_margin_start) * interpolatedTime));
        title.setLayoutParams(lpTitle);
    }

    @Override
    public void toggleSelectModeRecording(final View recording, boolean animate) {
        //https://stackoverflow.com/questions/13881419/android-change-left-margin-using-animation
        if(animate) {
            Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    modifyMargins(recording, interpolatedTime);
                }
            };
            animation.setDuration(500);
            recording.startAnimation(animation);
        }
        else
            modifyMargins(recording, 1);
    }


    @Override
    public void selectRecording(@NonNull android.view.View recording) {
        CheckBox checkBox = recording.findViewById(R.id.recording_checkbox);
        checkBox.setChecked(true);
//        if(getParentActivity().getSettedTheme().equals(TemplateActivity.LIGHT_THEME))
//            recording.setBackgroundColor(getResources().getColor(R.color.lightRecordingSelected));
//        else
//            recording.setBackgroundColor(getResources().getColor(R.color.darkRecordingSelected));
    }

    @Override
    public void deselectRecording(View recording) {
        CheckBox checkBox = recording.findViewById(R.id.recording_checkbox);
        checkBox.setChecked(false);
//        if(getParentActivity().getSettedTheme().equals(TemplateActivity.LIGHT_THEME))
//            card.setCardBackgroundColor(getResources().getColor(R.color.lightRecordingNotSelected));
//        else
//            card.setCardBackgroundColor(getResources().getColor(R.color.darkRecordingNotSelected));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setDetailsButtonsListeners();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(SELECT_MODE_KEY, selectMode);
        outState.putIntegerArrayList(SELECTED_ITEMS_KEY, (ArrayList<Integer>) selectedItems);
    }

    public static ContactDetailFragment newInstance(Contact contact) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_CONTACT, contact);
        ContactDetailFragment fragment = new ContactDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void setDetailsButtonsListeners() {
        ImageButton navigateBack = parentActivity.findViewById(R.id.navigate_back);
        navigateBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavUtils.navigateUpFromSameTask(parentActivity);
            }
        });
        final ImageButton menuButton = parentActivity.findViewById(R.id.phone_number_detail_menu);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//pentru micșorarea fontului se folosește constructorul PopupMenu(ContextThemeWrapper, v). E necesar un wrapper
// în jurul unui stil din styles.xml. Stilul trebuie să moștenească din Theme.AppCompat.Light.NoActionBar
// pentru temele light și din Theme.AppCompat.NoActionBar pentru cele dark, altfel background-ul
// va avea culoarea greșită.
                PopupMenu popupMenu = new PopupMenu(parentActivity,v);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId())
                        {
                            case R.id.delete_phone_number:
                                presenter.deleteContact(contact);
                                return true;
                            case R.id.should_record:
                                presenter.toggleShouldRecord(contact);
                            default:
                                return false;
                        }
                    }
                });
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.phone_number_popup, popupMenu.getMenu());
                MenuItem shouldRecordMenuItem = popupMenu.getMenu().findItem(R.id.should_record);
                if(contact.shouldRecord())
                    shouldRecordMenuItem.setTitle(R.string.stop_recording);
                else
                    shouldRecordMenuItem.setTitle(R.string.start_recording);
                if(contact.isPrivateNumber())
                    shouldRecordMenuItem.setEnabled(false);
                popupMenu.show();
            }
        });

        ImageButton editContact = parentActivity.findViewById(R.id.edit_contact);
        ImageButton callContact = parentActivity.findViewById(R.id.call_contact);
        if(contact.isPrivateNumber()) {
            callContact.setVisibility(View.GONE);
            editContact.setVisibility(View.GONE);
        }
        else {
            callContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   presenter.callContact(contact);
                }
            });

            editContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    presenter.editContact(contact);
                }
            });

        }
        ImageButton closeBtn = parentActivity.findViewById(R.id.close_select_mode);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSelectedMode();
            }
        });

        ImageButton deleteRecording = parentActivity.findViewById(R.id.actionbar_select_delete);
        deleteRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(parentActivity)
                        .title(R.string.delete_recording_confirm_title)
                        .content(String.format(getResources().getString(
                                R.string.delete_recording_confirm_message), selectedItems.size()))
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .icon(parentActivity.getResources().getDrawable(R.drawable.warning))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                presenter.deleteSelectedRecordings();
                            }
                        })
                        .show();
            }
        });

        ImageButton exportBtn = parentActivity.findViewById(R.id.actionbar_select_export);
        registerForContextMenu(exportBtn);
        //foarte necesar. Altfel meniul contextual va fi arătat numai la long click.
        exportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.showContextMenu();
            }
        });

        ImageButton selectAllBtn = parentActivity.findViewById(R.id.actionbar_select_all);
        selectAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.toggleSelectAll();
            }
        });
        ImageButton infoBtn = parentActivity.findViewById(R.id.actionbar_info);
        infoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               presenter.onInfoClick();
            }
        });
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = parentActivity.getMenuInflater();
        inflater.inflate(R.menu.storage_chooser_options, menu);

        boolean allowMovePrivate = true;
        for(Recording recording : getSelectedRecordings())
            if(recording.isSavedInPrivateSpace()) {
                allowMovePrivate = false;
                break;
            }
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            int end = spanString.length();
            spanString.setSpan(new RelativeSizeSpan(0.87f), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(spanString);
        }

        MenuItem menuItem = menu.getItem(0);
        menuItem.setEnabled(allowMovePrivate);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.private_storage: presenter.moveSelectedRecordings(parentActivity.
                    getFilesDir().getAbsolutePath());
                return true;
            case R.id.public_storage:
                Content content = new Content();
                content.setOverviewHeading(parentActivity.getResources().getString(R.string.export_heading));
                StorageChooser.Theme theme = new StorageChooser.Theme(parentActivity);
                theme.setScheme(parentActivity.getResources().getIntArray(R.array.storage_chooser_theme));

                StorageChooser chooser = new StorageChooser.Builder()
                        .withActivity(parentActivity)
                        .withFragmentManager(parentActivity.getFragmentManager())
                        .allowCustomPath(true)
                        .setType(StorageChooser.DIRECTORY_CHOOSER)
                        .withMemoryBar(true)
                        .allowAddFolder(true)
                        .showHidden(true)
                        .withContent(content)
                        .setTheme(parentActivity.getSettedTheme().equals(TemplateActivity.DARK_THEME) ?
                                theme : null)
                        .build();

                chooser.show();

                chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
                    @Override
                    public void onSelect(String path) {
                        presenter.moveSelectedRecordings(path);
                    }
                });
                return true;

            default: return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new ContactDetailPresenter(this);
        adapter = new RecordingAdapter(new ArrayList<Recording>(0));
        Bundle args = getArguments();
        if(args != null)
            contact = args.getParcelable(ARG_CONTACT);

        if(savedInstanceState != null) {
            selectMode = savedInstanceState.getBoolean(SELECT_MODE_KEY);
            selectedItems = savedInstanceState.getIntegerArrayList(SELECTED_ITEMS_KEY);
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        detailView = (RelativeLayout) inflater.inflate(R.layout.contact_detail_fragment, container, false);
        typePhoneView = detailView.findViewById(R.id.phone_type_detail);
        phoneNumberView = detailView.findViewById(R.id.phone_number_detail);
        contactPhotoView = detailView.findViewById(R.id.contact_photo_detail);
        recordingStatusView = detailView.findViewById(R.id.recording_status);
        recordingsRecycler = detailView.findViewById(R.id.recordings);
        //workaround necesar pentru că, dacă recyclerul cu recordinguri conține imagini poza asta devine neagră.
        // Se pare că numai pe lolipop, de verificat. https://github.com/hdodenhof/CircleImageView/issues/31
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            contactPhotoView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//        calculateCardViewDimensions();
        recordingsRecycler.setLayoutManager(new LinearLayoutManager(parentActivity));
        recordingsRecycler.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));
        recordingsRecycler.setAdapter(adapter);
        toggleSelectModeActionBar(false);

        return detailView;
    }

    private Spanned getSpannedText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
        } else
            return Html.fromHtml(text);

    }

    @Override
    public void paintViews(List<Recording> recordings){
        typePhoneView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_phonetype_intro), contact.getPhoneTypeName())));
        phoneNumberView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_phonenumber_intro), contact.getPhoneNumber())));

        if(contact.getPhotoUri() != null) {
            contactPhotoView.clearColorFilter();
            contactPhotoView.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
            contactPhotoView.setImageURI(contact.getPhotoUri());
        }
        else {
            if(contact.isPrivateNumber())
                contactPhotoView.setImageResource(R.drawable.incognito);
            else {
                contactPhotoView.setImageResource(R.drawable.user_contact);
                contactPhotoView.setColorFilter(new
                        PorterDuffColorFilter(contact.getColor(), PorterDuff.Mode.LIGHTEN));
            }
        }
        displayRecordingStatus();

        TextView noContent = detailView.findViewById(R.id.no_content);
        adapter.replaceData(recordings);

        if(recordings.size() > 0)
            noContent.setVisibility(View.GONE);
        else
            noContent.setVisibility(View.VISIBLE);
    }


    public void displayRecordingStatus(){
        if(contact.isPrivateNumber()) {
            recordingStatusView.setVisibility(View.INVISIBLE);
            return;
        }
        if(contact.shouldRecord()) {
            recordingStatusView.setText(R.string.rec_status_recording);
            recordingStatusView.setTextColor(getResources().getColor(R.color.green));
        }
        else {
            recordingStatusView.setText(R.string.rec_status_not_recording);
            recordingStatusView.setTextColor(getResources().getColor(R.color.red));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        if(resultCode == Activity.RESULT_OK && requestCode == ContactDetailPresenter.EDIT_REQUEST_CODE) {
            presenter.onEditActivityResult(intent.getExtras());
        }
    }

    class RecordingHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView title;
        ImageView recordingType;
        CheckBox checkBox;

        RecordingHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.recording, parent, false));
            recordingType = itemView.findViewById(R.id.recording_type);
            title = itemView.findViewById(R.id.recording_title);
            checkBox = itemView.findViewById(R.id.recording_checkbox);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            presenter.selectRecording(v, this.getAdapterPosition());
            return true;
        }

        @Override
        public void onClick(View v) {
            if(isSelectModeOn()) {
                presenter.selectRecording(v, this.getAdapterPosition());
            }
            else { //usual short click
                presenter.startPlayerActivity(((RecordingAdapter) recordingsRecycler.getAdapter()).
                        getItem(getAdapterPosition()));
            }
        }
    }

    class RecordingAdapter extends RecyclerView.Adapter<RecordingHolder> {
        List<Recording> recordings;

        void replaceData(List<Recording> recordings) {
            this.recordings = recordings;
            notifyDataSetChanged();
        }

        RecordingAdapter(List<Recording> recordings) {
            this.recordings = recordings;
        }

        @Override
        @NonNull
        public RecordingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parentActivity);
            return new RecordingHolder(layoutInflater, parent);
        }

        Recording getItem(int position) {
            return recordings.get(position);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordingHolder holder, final int position) {
            Recording recording = recordings.get(position);
            holder.title.setText("Recording " + recording.getDate() + " " + recording.getTime());
            holder.recordingType.setImageResource(recording.isIncoming() ? R.drawable.incoming : R.drawable.outgoing);
            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.selectRecording(view, position);
                }
            });
            //pentru situația cînd este întors ecranul sau cînd activitatea trece
            // în background sau cînd se scrolează lista de recordinguri.

                toggleSelectModeRecording(holder.itemView, false);
                if(selectedItems.contains(position))
                    selectRecording(holder.itemView);
                else
                    deselectRecording(holder.itemView);

//            if(selectedItems.contains(position))
//                selectRecording(holder.itemView);
//            else
//                deselectRecording(holder.itemView);
        }

        @Override
        public int getItemCount() {
            return recordings.size();
        }

    }
}
