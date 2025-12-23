from PIL import Image   # conda install pillow
import numpy as np
import matplotlib.pyplot as plt
import re

def hex_to_image(hex_text, upscale=32, save_path="output.png"):
    """
    Convert a pasted block of hex bytes into a rendered grayscale image
    and save it as a PNG file.
    """
    # Extract all hex byte tokens (00, FF, 7A, etc.)
    tokens = re.findall(r"[0-9A-Fa-f]{2}", hex_text)
    if not tokens:
        raise ValueError("No hex bytes found in input!")

    # Convert to uint8 list
    values = [int(t, 16) for t in tokens]

    # Infer square size if possible
    length = len(values)
    side = int(length**0.5)

    if side * side != length:
        print(f"Warning: data is {length} bytes, not a perfect square.")
        print("Using height=number of lines, width=bytes per line.")

        # Determine width from first line
        first_line = hex_text.strip().splitlines()[0]
        w = len(re.findall(r"[0-9A-Fa-f]{2}", first_line))
        h = length // w
        arr = np.array(values, dtype=np.uint8).reshape((h, w))
    else:
        arr = np.array(values, dtype=np.uint8).reshape((side, side))

    # Make image
    img = Image.fromarray(arr, mode='L')

    # Upscale for visibility
    big = img.resize((img.width * upscale, img.height * upscale), Image.NEAREST)

    # Save PNG
    big.save(save_path)
    print(f"Saved image to: {save_path}")

    # Also display
    plt.imshow(big, cmap='gray', vmin=0, vmax=255)
    plt.axis('off')
    plt.show()


# -----------------------------
# Paste the printed content from "Decompressed Output:" here
# -----------------------------
hex_input = """
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
"""

hex_to_image(hex_input, upscale=32, save_path="output.png")
