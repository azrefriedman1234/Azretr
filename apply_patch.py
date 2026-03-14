from pathlib import Path
import re

ROOT = Path('.')


def write(path: str, content: str):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding='utf-8')


def patch_text_tag(tag: str) -> str:
    if 'android:textColor=' in tag:
        tag = re.sub(r'android:textColor="[^"]*"', 'android:textColor="#FFFFFF"', tag)
    else:
        tag = tag.replace('/>', ' android:textColor="#FFFFFF"/>')
    if 'android:textColorHint=' in tag:
        tag = re.sub(r'android:textColorHint="[^"]*"', 'android:textColorHint="#F3D5DA"', tag)
    else:
        tag = tag.replace('/>', ' android:textColorHint="#F3D5DA"/>')
    return tag


def patch_details_xml(p: Path):
    s = p.read_text(encoding='utf-8')
    m = re.search(r'<EditText\b[^>]*android:id="@\+id/etCaption"[\s\S]*?/>', s, re.S)
    if m:
        tag = m.group(0)
        tag = patch_text_tag(tag)
        if 'android:backgroundTint=' in tag:
            tag = re.sub(r'android:backgroundTint="[^"]*"', 'android:background="@drawable/bg_editor_red"', tag)
        elif 'android:background=' not in tag:
            tag = tag.replace('/>', ' android:background="@drawable/bg_editor_red"/>')
        tag = re.sub(r'android:layout_height="[^"]*"', 'android:layout_height="340dp"', tag)
        if 'android:minLines=' in tag:
            tag = re.sub(r'android:minLines="[^"]*"', 'android:minLines="9"', tag)
        if 'android:maxLines=' in tag:
            tag = re.sub(r'android:maxLines="[^"]*"', 'android:maxLines="16"', tag)
        s = s.replace(m.group(0), tag, 1)
    if '@+id/appKeyboard' not in s and '@+id/etCaption' in s:
        s = re.sub(r'(<EditText\b[^>]*android:id="@\+id/etCaption"[\s\S]*?/>)',
                   r'\1\n\n                    <com.pasiflonet.mobile.ui.AppHebrewKeyboardView\n                        android:id="@+id/appKeyboard"\n                        android:layout_width="match_parent"\n                        android:layout_height="wrap_content" />',
                   s, count=1)
    p.write_text(s, encoding='utf-8')


def patch_settings_xml(p: Path):
    s = p.read_text(encoding='utf-8')
    m = re.search(r'<EditText\b[^>]*android:id="@\+id/etSignature"[\s\S]*?/>', s, re.S)
    if m:
        tag = patch_text_tag(m.group(0))
        s = s.replace(m.group(0), tag, 1)
    if '@+id/appKeyboardSettings' not in s and '@+id/etSignature' in s:
        s = re.sub(r'(<EditText\b[^>]*android:id="@\+id/etSignature"[\s\S]*?/>)',
                   r'\1\n\n                <com.pasiflonet.mobile.ui.AppHebrewKeyboardView\n                    android:id="@+id/appKeyboardSettings"\n                    android:layout_width="match_parent"\n                    android:layout_height="wrap_content" />',
                   s, count=1)
    p.write_text(s, encoding='utf-8')


def patch_details_kt(p: Path):
    s = p.read_text(encoding='utf-8')
    if 'import android.view.View' not in s and 'import android.os.Bundle' in s:
        s = s.replace('import android.os.Bundle', 'import android.os.Bundle\nimport android.view.View', 1)
    if 'b.appKeyboard.bindTo(b.etCaption)' not in s and 'setContentView(b.root)' in s:
        s = s.replace('setContentView(b.root)', 'setContentView(b.root)\n        b.appKeyboard.bindTo(b.etCaption)\n        b.appKeyboard.visibility = View.VISIBLE', 1)
    p.write_text(s, encoding='utf-8')


def patch_settings_kt(p: Path):
    s = p.read_text(encoding='utf-8')
    if 'import android.view.View' not in s and 'import android.os.Bundle' in s:
        s = s.replace('import android.os.Bundle', 'import android.os.Bundle\nimport android.view.View', 1)
    if 'b.appKeyboardSettings.bindTo(b.etSignature)' not in s and 'setContentView(b.root)' in s:
        s = s.replace('setContentView(b.root)', 'setContentView(b.root)\n            b.appKeyboardSettings.bindTo(b.etSignature)\n            b.appKeyboardSettings.visibility = View.VISIBLE', 1)
    p.write_text(s, encoding='utf-8')


def compact_item_layout(p: Path):
    s = p.read_text(encoding='utf-8')
    s = re.sub(r'app:cardCornerRadius="[^"]+"', 'app:cardCornerRadius="6dp"', s)
    s = re.sub(r'app:cornerRadius="[^"]+"', 'app:cornerRadius="6dp"', s)
    rep = {
        'android:layout_marginBottom="20dp"':'android:layout_marginBottom="4dp"',
        'android:layout_marginBottom="16dp"':'android:layout_marginBottom="4dp"',
        'android:layout_marginBottom="14dp"':'android:layout_marginBottom="4dp"',
        'android:layout_marginBottom="12dp"':'android:layout_marginBottom="4dp"',
        'android:layout_marginBottom="8dp"':'android:layout_marginBottom="4dp"',
        'android:layout_marginTop="20dp"':'android:layout_marginTop="4dp"',
        'android:layout_marginTop="16dp"':'android:layout_marginTop="4dp"',
        'android:layout_marginTop="14dp"':'android:layout_marginTop="4dp"',
        'android:layout_marginTop="12dp"':'android:layout_marginTop="4dp"',
        'android:layout_marginTop="8dp"':'android:layout_marginTop="4dp"',
        'android:padding="20dp"':'android:padding="6dp"',
        'android:padding="18dp"':'android:padding="6dp"',
        'android:padding="16dp"':'android:padding="6dp"',
        'android:padding="14dp"':'android:padding="6dp"',
        'android:padding="12dp"':'android:padding="6dp"',
        'android:padding="10dp"':'android:padding="6dp"',
        'android:layout_height="48dp"':'android:layout_height="32dp"',
        'android:layout_height="46dp"':'android:layout_height="32dp"',
        'android:layout_height="44dp"':'android:layout_height="32dp"',
        'android:layout_height="42dp"':'android:layout_height="32dp"',
        'android:layout_height="40dp"':'android:layout_height="30dp"',
    }
    for old, new in rep.items():
        s = s.replace(old, new)
    for old, new in [('android:textSize="18sp"','android:textSize="15sp"'),('android:textSize="17sp"','android:textSize="14sp"'),('android:textSize="16sp"','android:textSize="13sp"'),('android:textSize="15sp"','android:textSize="12sp"'),('android:textSize="14sp"','android:textSize="12sp"')]:
        s = s.replace(old, new)
    p.write_text(s, encoding='utf-8')


def patch_main_xml(p: Path):
    s = p.read_text(encoding='utf-8')
    s = re.sub(r'app:cardCornerRadius="[^"]+"', 'app:cardCornerRadius="6dp"', s)
    s = re.sub(r'app:cornerRadius="[^"]+"', 'app:cornerRadius="6dp"', s)
    rep = {
        'android:padding="10dp"':'android:padding="6dp"',
        'android:paddingStart="8dp"':'android:paddingStart="4dp"',
        'android:paddingTop="8dp"':'android:paddingTop="4dp"',
        'android:paddingEnd="8dp"':'android:paddingEnd="4dp"',
        'android:paddingBottom="8dp"':'android:paddingBottom="4dp"',
        'android:layout_marginBottom="8dp"':'android:layout_marginBottom="4dp"',
        'android:layout_marginBottom="6dp"':'android:layout_marginBottom="4dp"',
        'android:layout_marginTop="10dp"':'android:layout_marginTop="4dp"',
        'android:layout_marginTop="8dp"':'android:layout_marginTop="4dp"',
        'android:layout_height="42dp"':'android:layout_height="34dp"',
        'android:layout_height="40dp"':'android:layout_height="32dp"',
        'android:layout_width="8dp"':'android:layout_width="4dp"',
        'android:layout_width="10dp"':'android:layout_width="4dp"',
    }
    for old, new in rep.items():
        s = s.replace(old, new)
    s = s.replace('android:textSize="17sp"','android:textSize="15sp"')
    s = s.replace('android:textSize="12sp"','android:textSize="11sp"')
    s = s.replace('android:textSize="11sp"','android:textSize="10sp"')
    p.write_text(s, encoding='utf-8')


def main():
    details_xml = ROOT / 'app/src/main/res/layout/activity_details.xml'
    settings_xml = ROOT / 'app/src/main/res/layout/activity_settings.xml'
    main_xml = ROOT / 'app/src/main/res/layout/activity_main.xml'
    details_kt = ROOT / 'app/src/main/java/com/pasiflonet/mobile/DetailsActivity.kt'
    settings_kt = ROOT / 'app/src/main/java/com/pasiflonet/mobile/SettingsActivity.kt'
    item1 = ROOT / 'app/src/main/res/layout/item_message_row.xml'
    item2 = ROOT / 'app/src/main/res/layout/item_chat.xml'

    if details_xml.exists():
        patch_details_xml(details_xml)
    if settings_xml.exists():
        patch_settings_xml(settings_xml)
    if main_xml.exists():
        patch_main_xml(main_xml)
    if details_kt.exists():
        patch_details_kt(details_kt)
    if settings_kt.exists():
        patch_settings_kt(settings_kt)
    if item1.exists():
        compact_item_layout(item1)
    if item2.exists():
        compact_item_layout(item2)

    print('Patch applied')


if __name__ == '__main__':
    main()
