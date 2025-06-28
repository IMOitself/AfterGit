package imo.after_git;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import imo.after_run.CommandTermux;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity 
{
    Button statusBtn;
    String repoPath = "";
    boolean isStop = false;
    boolean canRefreshStatus = false;
    AlertDialog commitDialog;
    AlertDialog diffDialog;
    AlertDialog gitLogsDialog;
    AlertDialog configDialog;
    AlertDialog fixGitDialog;
    AlertDialog gitLogItemDescDialog;
    
    static class gitStatusShort {
        static String branchStatus = "";
        static String[] changedFiles = {};
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		CommandTermux.checkAndRequestPermissions(this);
        
		
		final EditText repoPathEdit = findViewById(R.id.repo_path_edittext);
		statusBtn = findViewById(R.id.status_btn);
        final Button commitBtn = findViewById(R.id.commit_btn);
        final Button pullBtn = findViewById(R.id.pull_btn);
        final Button pushBtn = findViewById(R.id.push_btn);
        final Button gitLogBtn = findViewById(R.id.history_btn);
		final TextView outputTxt = findViewById(R.id.output_txt);
        commitBtn.setVisibility(View.GONE);
        pullBtn.setVisibility(View.GONE);
        pushBtn.setVisibility(View.GONE);
        gitLogBtn.setEnabled(false);
        
        repoPathEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    outputTxt.setText("");
                    commitBtn.setVisibility(View.GONE);
                    pullBtn.setVisibility(View.GONE);
                    pushBtn.setVisibility(View.GONE);
                    gitLogBtn.setEnabled(false);
                }
            });
        
		statusBtn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
                outputTxt.setText("");
                commitBtn.setVisibility(View.GONE);
                pullBtn.setVisibility(View.GONE);
                pushBtn.setVisibility(View.GONE);
                gitLogBtn.setEnabled(false);
                
				repoPath = repoPathEdit.getText().toString().trim();
                
                Runnable onEnd = new Runnable(){
                    @Override
                    public void run(){
                        boolean doPull = gitStatusShort.branchStatus.contains("behind");
                        boolean doPush = gitStatusShort.branchStatus.contains("ahead");
                        boolean doCommit = gitStatusShort.changedFiles.length != 0;
                        
                        pullBtn.setVisibility(doPull ? View.VISIBLE : View.GONE);
                        pushBtn.setVisibility(doPush ? View.VISIBLE : View.GONE);
                        commitBtn.setVisibility(doCommit ? View.VISIBLE : View.GONE);
                        gitLogBtn.setEnabled(true);
                    }
                };
                
                runGitStatus(repoPath, outputTxt, onEnd);
			}
		});
        
        commitBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    
                    Runnable onAfterCommit = new Runnable(){
                        @Override
                        public void run(){
                            outputTxt.setText("");
                            commitBtn.setVisibility(View.GONE);
                            pullBtn.setVisibility(View.GONE);
                            pushBtn.setVisibility(View.GONE);
                            gitLogBtn.setEnabled(false);
                        }
                    };
                    
                    commitDialog = makeCommitDialog(repoPath, gitStatusShort.changedFiles, onAfterCommit);
                    commitDialog.show();
                }
            });
            
        pullBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    //TODO: run git pull
                    makeComingSoonDialog().show();
                }
            });
            
        pushBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    //TODO: run git push
                    makeComingSoonDialog().show();
                }
            });
            
        gitLogBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    final String savedTextString = gitLogBtn.getText().toString();
                    
                    String command = "cd " + repoPath;
                    command += "\ngit log --oneline --graph --pretty=format:\"-%h-%s\"";
                    // machine readable format for each log:
                    // * -0ha45sh-commit message here

                    new CommandTermux(command, MainActivity.this)
                        .setOnEnd(new Runnable(){
                            @Override
                            public void run(){
                                String output = CommandTermux.getOutput();
                                
                                gitLogsDialog = makeGitLogsDialog(repoPath, output);
                                gitLogsDialog.show();
                                gitLogBtn.setText(savedTextString);
                            }
                        })
                        .setLoading(gitLogBtn)
                        .run();
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isStop || !canRefreshStatus) return;
        statusBtn.performClick();//refresh status
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStop = true;
        if(commitDialog != null && commitDialog.isShowing())
            commitDialog.dismiss();
            
        if(diffDialog != null && diffDialog.isShowing())
            diffDialog.dismiss();
            
        if(configDialog != null && configDialog.isShowing())
            configDialog.dismiss();
            
        if(fixGitDialog != null && fixGitDialog.isShowing())
            fixGitDialog.dismiss();
            
        if(gitLogsDialog != null && gitLogsDialog.isShowing())
            gitLogsDialog.dismiss();
            
        if(gitLogItemDescDialog != null && gitLogItemDescDialog.isShowing())
            gitLogItemDescDialog.dismiss();
    }
    
    
    
    
    
    
    void runGitStatus(final String repoPath, final TextView outputTxt, final Runnable onEnd){
        final String commandDivider = "LONG STATUS ABOVE. SHORT STATUS BELOW.";
        String command = "cd " + repoPath;
        command += "\ngit status --long";
        command += "\necho \""+ commandDivider+"\"";
        command += "\ngit status --short --branch";

        new CommandTermux(command, MainActivity.this)
            .setOnEnd(new Runnable(){
                @Override
                public void run(){
                    String output = CommandTermux.getOutput();
                    
                    if(! isGitWorking(output)){
                        fixGit(output, repoPath);
                        return;
                    }
                    
                    if(! isRepository(output, outputTxt)) return;
                    
                    String[] outputParts = output.split(commandDivider);
                    
                    String statusLong = outputParts[0];
                    String statusShort = outputParts[1];
                    
                    String[] statusShortParts = statusShort.trim().split("\n");
                    gitStatusShort.branchStatus = statusShortParts[0];
                    gitStatusShort.changedFiles = Arrays.copyOfRange(statusShortParts, 1, statusShortParts.length);
                    
                    outputTxt.setText(statusLong);
                    onEnd.run();
                }
            })
            .setOnError(new Runnable(){
                @Override
                public void run(){
                    outputTxt.setText("try again");
                }
            })
            .setLoading(outputTxt)
            .run();
    }
    
    boolean isRepository(String commandOutput, TextView textview){
        if(commandOutput.contains("cd: can't cd")){
            textview.setText("not a folder path");
            canRefreshStatus = false;
            return false;
        }
        if(commandOutput.contains("fatal: not a git repository")){
            textview.setText("not a git repository");
            canRefreshStatus = false;
            return false;
        }
        canRefreshStatus = true;
        return true;
    }
    
    
    
    
    
    
    AlertDialog makeCommitDialog(final String repoPath, String[] changedFiles, final Runnable onAfterCommit){
        String title = "Commit Changes";
        LinearLayout layout = new LinearLayout(this);
        ListView changesList = new ListView(this);
        final EditText commitMessageEdit = new EditText(this);
        final CheckBox amendCheckbox = new CheckBox(this);
        CheckBox stageAllFilesCheckbox = new CheckBox(this);
        
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(commitMessageEdit);
        layout.addView(amendCheckbox);
        layout.addView(stageAllFilesCheckbox);
        layout.addView(changesList);
        
        changesList.setAdapter(new CommitChangesAdapter(MainActivity.this, repoPath, changedFiles));
        commitMessageEdit.setHint("commit message...");
        amendCheckbox.setText("Amend previous commit");
        stageAllFilesCheckbox.setText("Stage all files");
        stageAllFilesCheckbox.setChecked(true);
        stageAllFilesCheckbox.setEnabled(false);// cannot be change
        
        final AlertDialog commitDialog = new AlertDialog.Builder(MainActivity.this)
            .setTitle(title)
            .setView(layout)
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dia, int which) {
                    dia.dismiss();
                }
            })
            .setPositiveButton("Commit", null)
            .create();
        
        amendCheckbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton v, boolean isChecked){
                if(! isChecked) commitMessageEdit.setText("");
                if(isChecked) getLatestCommit(repoPath, commitMessageEdit);
            }
        });
        
        commitDialog.setOnShowListener(new DialogInterface.OnShowListener(){
            @Override
            public void onShow(final DialogInterface dia){
                Button positiveButton = commitDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View v){
                            String commitMessage = commitMessageEdit.getText().toString();
                            
                            commit(commitMessage, amendCheckbox.isChecked());
                            
                            onAfterCommit.run();
                        }
                    });
            }
        });
        return commitDialog;
    }
    
    AlertDialog makeDiffDialog(final String repoPath, final String changedFile){
        return makeDiffDialog(repoPath, changedFile, null);
    }
    
    AlertDialog makeDiffDialog(final String repoPath, final String changedFile, String commitHash){
        String title = "Diff";
        ScrollView scrollView = new ScrollView(this);
        final RelativeLayout linesLayout = new RelativeLayout(this);
        final LinearLayout lineBackgroundsLayout = new LinearLayout(this);
        final TextView linesText = new TextView(this);
        
        final int textSize = 11;
        final Typeface typeface = Typeface.MONOSPACE;
        
        linesText.setTextSize(textSize);
        linesText.setTypeface(typeface);
        linesText.setTextIsSelectable(true);
        linesText.setLayoutParams(new ViewGroup.LayoutParams(
                                     ViewGroup.LayoutParams.MATCH_PARENT,
                                     ViewGroup.LayoutParams.WRAP_CONTENT
                                 ));
        
        lineBackgroundsLayout.setOrientation(LinearLayout.VERTICAL);
        
        linesLayout.addView(lineBackgroundsLayout);
        linesLayout.addView(linesText);
        scrollView.addView(linesLayout);
        
        boolean viewUnsavedChangesMode = commitHash == null;
        
        String command = "cd " + repoPath;
        
        command += viewUnsavedChangesMode ? 
            "\ngit diff HEAD -- " + changedFile :
            "\ngit diff "+commitHash+"^ "+commitHash+" -- " + changedFile;
        
        new CommandTermux(command, MainActivity.this)
            .setOnEnd(new Runnable(){
                @Override
                public void run(){
                    String output = CommandTermux.getOutput();
                    
                    String outputWithoutHeader = "";
                    
                    if(output.contains("@@"))
                        outputWithoutHeader = output.substring(output.indexOf("@@"));
                    else
                        outputWithoutHeader = "Binaries differ";
                    
                    addColoredDiffBgToText(outputWithoutHeader, linesText, lineBackgroundsLayout);
                }
            })
            .setLoading(linesText)
            .run();
            
        return new AlertDialog.Builder(MainActivity.this)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .create();
    }
    
    AlertDialog makeGitLogsDialog(String repoPath, String gitLogOutput){
        ListView gitLogsList = new ListView(this);
        
        List<GitLog> gitLogs = new ArrayList<>();
        
        String currGraphSymbol = "";
        
        for(String gitLogString : gitLogOutput.split("\n")){
            GitLog gitLog = new GitLog(gitLogString);
            
            //combine graphs and prevent empty log
            if(gitLog.commitHash.isEmpty()){
                currGraphSymbol += gitLog.graphSymbols + "\n";
                continue;
            }
            gitLog.graphSymbols = currGraphSymbol + gitLog.graphSymbols;
            currGraphSymbol = "";
            
            gitLogs.add(gitLog);
        }
        
        gitLogsList.setAdapter(new GitLogAdapter(MainActivity.this, gitLogs));
        
        return new AlertDialog.Builder(MainActivity.this)
            .setTitle("Log")
            .setView(gitLogsList)
            .setPositiveButton("Close", null)
            .create();
    }
    
    AlertDialog makeGitLogItemDescDialog(final String repoPath, final GitLog gitLog){
        String title = "Commit";
        final LinearLayout layout = new LinearLayout(this);
        final TextView textview = new TextView(this);
        
        int paddingDp = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8, //dp
            getResources().getDisplayMetrics()
        );
        layout.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(textview);
        
        final String commandDivider = "COMMIT DESC ABOVE. CHANGED FILES BELOW.";
        
        String command = "cd " + repoPath;
        command += "\ngit show -s --pretty=format:\"%B%n%ncommit hash: %h%nauthor: %an%ndate: %cd%n- %cr\" "+gitLog.commitHash;
        command += "\necho \""+ commandDivider+"\"";
        command += "\ngit diff-tree --no-commit-id --name-status -r "+gitLog.commitHash;
        
        new CommandTermux(command, MainActivity.this)
            .setOnEnd(new Runnable(){
                @Override
                public void run(){
                    String output = CommandTermux.getOutput();
                    
                    String[] outputParts = output.split(commandDivider);
                    
                    String commitDesc = outputParts[0];
                    textview.setText(commitDesc);
                    
                    if (outputParts.length < 2) return;
                    
                    String[] changedFiles = outputParts[1].trim().split("\n");
                    
                    final ListView changesList = new ListView(MainActivity.this);
                    layout.addView(changesList);
                    
                    changesList.setAdapter(new CommitChangesAdapter(MainActivity.this, repoPath, changedFiles, gitLog.commitHash));
                    changesList.invalidate();
                }
            })
            .setLoading(textview)
            .run();
            
        return new AlertDialog.Builder(MainActivity.this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("Close", null)
            .create();
    }
    
    AlertDialog makeConfigDialog(final String repoPath){
        String title = "Configure Repository";
        LinearLayout layout = new LinearLayout(this);
        final EditText usernameEdit = new EditText(this);
        final EditText emailEdit = new EditText(this);
        
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(usernameEdit);
        layout.addView(emailEdit);
        usernameEdit.setHint("user name...");
        emailEdit.setHint("user email...");

        return new AlertDialog.Builder(MainActivity.this)
            .setTitle(title)
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dia, int which) {
                    String username = usernameEdit.getText().toString();
                    String email = emailEdit.getText().toString();
                    String command = "cd " + repoPath;
                    command += "\ngit config user.name \""+username+"\"";
                    command += "\ngit config user.email \""+email+"\"";
                    
                    new CommandTermux(command, MainActivity.this).run();
                    dia.dismiss();
                }
            })
            .create();
    }
    
    AlertDialog makeFixGitDialog(String dialogTitle, String dialogMessage, String stringToCopy){
        LinearLayout messageLayout = new LinearLayout(MainActivity.this);
        TextView messageText = new TextView(MainActivity.this);
        Button copyBtn = new Button(MainActivity.this);

        messageText.setText(dialogMessage);
        copyBtn.setText("Copy");
        final String copyString = stringToCopy;

        copyBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard == null) return;
                    clipboard.setPrimaryClip(ClipData.newPlainText("Copied Text", copyString));

                    Toast.makeText(MainActivity.this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            });

        messageLayout.setOrientation(LinearLayout.VERTICAL);
        messageLayout.addView(messageText);
        messageLayout.addView(copyBtn);

        return new AlertDialog.Builder(MainActivity.this)
            .setTitle(dialogTitle)
            .setView(messageLayout)
            .setPositiveButton("Open Termux", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dia, int which) {
                    try {
                        CommandTermux.openTermux(MainActivity.this);
                    } catch(Exception e) {}
                }
            })
            .create();
    }
    
    AlertDialog makeComingSoonDialog(){
        return new AlertDialog.Builder(MainActivity.this)
            .setTitle("Coming Soon")
            .setMessage("will add this feature in the future :D")
            .setNeutralButton("Look For Updates", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dia, int which) {
                    String url = "https://github.com/IMOitself/AfterGit";
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
            })
            .setPositiveButton("Ok", null)
            .create();
    }
    
    
    
    
    
    void commit(String commitMessage, boolean isAmend){
        if(commitMessage.trim().isEmpty()) {
            Toast.makeText(MainActivity.this, "Enter commit message", Toast.LENGTH_SHORT).show();
            return;
        }

        String command = "cd " + repoPath;
        command += "\ngit add .";
        command += "\ngit commit -m '"+commitMessage+"'";
        if(isAmend) command += " --amend --allow-empty";

        new CommandTermux(command, MainActivity.this)
            .setOnEnd(new Runnable(){
                @Override
                public void run(){
                    String output = CommandTermux.getOutput();

                    if(output.contains("fatal: unable to auto-detect")){
                        Toast.makeText(MainActivity.this, "configure user name and user email first:D", Toast.LENGTH_SHORT).show();
                        configDialog = makeConfigDialog(repoPath);
                        configDialog.show();
                        return;
                    }

                    Toast.makeText(MainActivity.this, "successfully commited:D", Toast.LENGTH_SHORT).show();
                    commitDialog.dismiss();
                }
            })
            .run();
    }
    
    void getLatestCommit(String repoPath, TextView outputText){
        String command = "cd " + repoPath;
        command += "\ngit log -1 --pretty=%B";

        new CommandTermux(command, MainActivity.this)
            .quickSetOutputWithLoading(outputText)
            .setLoading(outputText)
            .run();
    }
    
    boolean isGitWorking(final String output){
        return ! output.contains("git: not found") || 
               ! output.contains("dubious ownership");
    }
    
    void fixGit(final String output, String repoPath){
        String dialogTitle = "";
        String dialogMessage = "";
        String stringToCopy = "";
        
        if(output.contains("git: not found")) {
            dialogTitle = "Git not installed";
            stringToCopy = "pkg install git -y && exit";
            dialogMessage = "paste this on Termux to install git:\n\n" + stringToCopy;
        }
        else
        if(output.contains("dubious ownership")){
            dialogTitle = "Repo not listed in safe directories";
            stringToCopy = "git config --global --add safe.directory " + repoPath + " && exit";
            dialogMessage = "paste this on Termux to add repo as safe directory:\n\n" + stringToCopy;
        }
        else{
            return;
        }
        
        fixGitDialog = makeFixGitDialog(dialogTitle, dialogMessage, stringToCopy);
        fixGitDialog.show();
    }
    
    void addColoredDiffBgToText(final String output, final TextView linesText, final LinearLayout backgroundLayout){
        linesText.post(new Runnable() {
                @Override
                public void run() {
                    linesText.setText("");
                    backgroundLayout.removeAllViews();

                    TextPaint textPaint = linesText.getPaint();
                    int availableWidth = linesText.getWidth() - linesText.getPaddingLeft() - linesText.getPaddingRight();

                    for (String line : output.trim().split("\n")) {
                        String backgroundColor = null;
                        
                        if(line.startsWith("@@")) backgroundColor = "#DDF3FE";
                        if(line.startsWith("+")) backgroundColor = "#DAFAE2";
                        if(line.startsWith("-")) backgroundColor = "#FFEBEA";

                        if(line.startsWith("+")) line = line.substring(1);
                        if(line.startsWith("-")) line = line.substring(1);

                        linesText.append(line + "\n");

                        StaticLayout staticLayout = new StaticLayout(
                            line,
                            textPaint,
                            availableWidth,
                            Layout.Alignment.ALIGN_NORMAL,
                            linesText.getLineSpacingMultiplier(),
                            linesText.getLineSpacingExtra(),
                            false);

                        int blockHeight = staticLayout.getHeight();

                        final View lineBackground = new View(MainActivity.this);

                        lineBackground.setLayoutParams(new ViewGroup.LayoutParams(
                                                           ViewGroup.LayoutParams.MATCH_PARENT,
                                                           blockHeight
                                                       ));

                        if (backgroundColor != null)
                            lineBackground.setBackgroundColor(Color.parseColor(backgroundColor));

                        backgroundLayout.addView(lineBackground);
                    }
                }
            });
    }
    
    
    
    
    
    
    
    class CommitChangesAdapter extends ArrayAdapter<String> {

        String repoPath = "";
        String commitHash;
        boolean viewUnsavedChangesMode = true;

        public CommitChangesAdapter(Context context, String repoPath, String[] changedFiles, String commitHash) {
            super(context, 0, changedFiles);
            this.repoPath = repoPath;
            this.commitHash = commitHash;
            this.viewUnsavedChangesMode = false;
        }
        
        public CommitChangesAdapter(Context context, String repoPath, String[] changedFiles) {
            super(context, 0, changedFiles);
            this.repoPath = repoPath;
            this.viewUnsavedChangesMode = true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textview;

            if (convertView != null) return (TextView) convertView;

            textview = new TextView(getContext());
            textview.setPadding(32, 32, 32, 32);
            textview.setTypeface(Typeface.MONOSPACE);

            String item = getItem(position);

            if(item == null && item.isEmpty()) return textview;

            String fileStateString = item.substring(0, 2).trim();
            final char fileState = fileStateString.charAt(0);
            final String filePath = item.substring(2).trim();

            String htmlString = "";
            
            if(fileState == 'M') htmlString = "<font color='#0C4EA2'>M</font> " + filePath;
            if(fileState == '?') htmlString = "<font color='#20883D'>+</font> " + filePath;
            if(fileState == 'A') htmlString = "<font color='#20883D'>+</font> " + filePath;
            if(fileState == 'D') htmlString = "<font color='#FF0000'>-</font> " + filePath;
            if(htmlString.isEmpty()) htmlString = item;

            textview.setText(Html.fromHtml(htmlString));
            textview.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        diffDialog = viewUnsavedChangesMode ?
                            makeDiffDialog(repoPath, filePath) : 
                            makeDiffDialog(repoPath, filePath, commitHash);
                        
                        diffDialog.show();
                    }
                });

            return textview;
        }
    }
    
    class GitLog {
        String graphSymbols = "";
        String commitHash = "";
        String commitMessage = "";

        GitLog(String gitLogString) {
            String[] parts = gitLogString.split("-", 3);
            if (parts.length > 0) this.graphSymbols = parts[0];
            if (parts.length > 1) this.commitHash = parts[1].trim();
            if (parts.length > 2) this.commitMessage = parts[2].trim();
        }
    }


    class GitLogAdapter extends ArrayAdapter<GitLog> {

        private class ViewHolder {
            TextView graphSymbolsText;
            TextView commitMessageText;
        }

        public GitLogAdapter(Context context, List<GitLog> gitLogs) {
            super(context, 0, gitLogs);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                Context context = getContext();
                ViewHolder viewHolder = new ViewHolder();

                LinearLayout rowLayout = new LinearLayout(context);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setGravity(Gravity.CENTER_VERTICAL);
                rowLayout.setLayoutParams(new AbsListView.LayoutParams(
                                              AbsListView.LayoutParams.MATCH_PARENT,
                                              AbsListView.LayoutParams.WRAP_CONTENT));
                int paddingDp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    8, //dp
                    getResources().getDisplayMetrics()
                );
                rowLayout.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
                rowLayout.setMinimumHeight(
                    (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        50, //dp
                        getResources().getDisplayMetrics()
                    ));

                viewHolder.graphSymbolsText = new TextView(context);
                viewHolder.graphSymbolsText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                viewHolder.graphSymbolsText.setTextColor(Color.parseColor("#03A9F4"));
                
                viewHolder.commitMessageText = new TextView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f);
                viewHolder.commitMessageText.setLayoutParams(params);
                
                rowLayout.addView(viewHolder.graphSymbolsText);
                rowLayout.addView(viewHolder.commitMessageText);

                convertView = rowLayout;
                convertView.setTag(viewHolder);
            }

            // at this point, convertView is guaranteed to be a valid view with a ViewHolder tag.
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();

            final GitLog gitLog = getItem(position);
            
            if(gitLog == null) return convertView;

            viewHolder.graphSymbolsText.setText(gitLog.graphSymbols);
            viewHolder.commitMessageText.setText(gitLog.commitMessage);
            convertView.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        gitLogItemDescDialog = makeGitLogItemDescDialog(repoPath, gitLog);
                        gitLogItemDescDialog.show();
                    }
                });

            return convertView;
        }
    }
}
