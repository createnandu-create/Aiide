package in.aitechapp.teluguoffline;

import java.util.Locale;

/**
 * Pure Java text preparation for the opt-in Pocket TTS / Pravega research adapter.
 * No text is sent to a network service. The Android TTS engine must be verified
 * separately and must not silently fall back to Google/system speech.
 */
public final class ExperimentalText {
  private ExperimentalText() {}

  public static String pravegaTelugu(String input) {
    if (input == null) throw new IllegalArgumentException("Text is required");
    String clean = input.trim().replace('\r', ' ').replace('\n', ' ');
    clean = clean.replaceAll("\\s+", " ");
    if (clean.length() == 0) throw new IllegalArgumentException("Text is empty");
    if (clean.length() > 600) throw new IllegalArgumentException("For this experimental preview, use up to 600 characters.");
    if (!containsTelugu(clean)) throw new IllegalArgumentException("Include Telugu script for the [te] research voice.");
    if (clean.startsWith("[te] ")) return clean;
    // The model card requires a tag on EVERY independent synthesis request.
    return "[te] " + clean;
  }

  public static boolean containsTelugu(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= '\u0C00' && c <= '\u0C7F') return true;
    }
    return false;
  }
}
