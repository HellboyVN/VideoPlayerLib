package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;

public class SettingsPreference extends Fragment implements View.OnClickListener {
    public static final String LOG_TAG = "SettingsPreference";
    private String mTitle = "More Apps";
    private Button btn_pencil, btn_buttlegs, btn_fitness, btn_abs;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_moreapp, container, false);
        btn_pencil = (Button) layout.findViewById(R.id.btn_pencil);
        btn_buttlegs = (Button) layout.findViewById(R.id.btn_buttlegs);
        btn_fitness = (Button) layout.findViewById(R.id.btn_fitness);
        btn_abs = (Button) layout.findViewById(R.id.btn_abs);
        btn_pencil.setOnClickListener(this);
        btn_buttlegs.setOnClickListener(this);
        btn_fitness.setOnClickListener(this);
        btn_abs.setOnClickListener(this);

        return layout;
    }

    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
        MainActivity act = (MainActivity) getActivity();
        if (act != null) {
            act.getSupportActionBar().setTitle(this.mTitle);
            act.setHomeAsBackArrow();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_pencil:
                try {
                    startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=com.hbtools.photo.sketchpencil.editer.pencilsketch")));
                } catch (ActivityNotFoundException e3) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.market_error), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_buttlegs:
                try {
                    startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=hb.legsandbuttocksworkout.homeworkout.gym.buttlegs.buttlegspro.buttlegschallenge")));
                } catch (ActivityNotFoundException e3) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.market_error), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_fitness:
                try {
                    startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=hb.fitnesschallenge.homeworkout.homeworkouts.noequipment.fitnesspro")));
                } catch (ActivityNotFoundException e3) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.market_error), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_abs:
                try {
                    startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=hb.abs.absworkout.bellyfatworkout.waistworkout.abdominalworkout.absworkoutpro")));
                } catch (ActivityNotFoundException e3) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.market_error), Toast.LENGTH_SHORT).show();
                }
                break;
            default: break;
        }
    }
}
