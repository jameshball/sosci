import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Sosci {

  private static final int NUM_VERTICES = 4000;
  private static final int VERTEX_SIZE = 2;

  private final AudioLoopback loopback;

  public Sosci(AudioLoopback loopback) {
    this.loopback = loopback;
    new Thread(loopback).start();
  }

  // The window handle
  private long window;

  public void run() {
    init();
    loop();

    // Free the window callbacks and destroy the window
    glfwFreeCallbacks(window);
    glfwDestroyWindow(window);

    // Terminate GLFW and free the error callback
    glfwTerminate();
    glfwSetErrorCallback(null).free();
  }

  private void init() {
    // Setup an error callback. The default implementation
    // will print the error message in System.err.
    GLFWErrorCallback.createPrint(System.err).set();

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW");

    // Configure GLFW
    glfwDefaultWindowHints(); // optional, the current window hints are already the default
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after  creation
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

    // Create the window
    window = glfwCreateWindow(640, 480, "sosci", NULL, NULL);
    if ( window == NULL )
      throw new RuntimeException("Failed to create the GLFW window");

    // Setup a key callback. It will be called every time a key is pressed, repeated or released.
    glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
      if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
        glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
    });

    // Get the thread stack and push a new frame
    try ( MemoryStack stack = stackPush() ) {
      IntBuffer pWidth = stack.mallocInt(1); // int*
      IntBuffer pHeight = stack.mallocInt(1); // int*

      // Get the window size passed to glfwCreateWindow
      glfwGetWindowSize(window, pWidth, pHeight);

      // Get the resolution of the primary monitor
      GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

      // Center the window
      glfwSetWindowPos(
        window,
        (vidmode.width() - pWidth.get(0)) / 2,
        (vidmode.height() - pHeight.get(0)) / 2
      );
    } // the stack frame is popped automatically

    // Make the OpenGL context current
    glfwMakeContextCurrent(window);
    // Enable v-sync
    glfwSwapInterval(1);

    // Make the window visible
    glfwShowWindow(window);
  }

  private void loop() {
    // This line is critical for LWJGL's interoperation with GLFW's
    // OpenGL context, or any context that is managed externally.
    // LWJGL detects the context that is current in the current thread,
    // creates the GLCapabilities instance and makes the OpenGL
    // bindings available for use.
    GL.createCapabilities();

    glfwWindowHint(GLFW_STENCIL_BITS, 8);
    glfwWindowHint(GLFW_SAMPLES, 8);

    FloatBuffer vertex_data = BufferUtils.createFloatBuffer(NUM_VERTICES * VERTEX_SIZE);
    for (int i = 0; i < NUM_VERTICES; i++) {
      vertex_data.put(new float[] { 0, 0 });
    }
    vertex_data.flip();

    int vbo_vertex_handle = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, vbo_vertex_handle);
    glBufferData(GL_ARRAY_BUFFER, vertex_data, GL_STATIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    // Run the rendering loop until the user has attempted to close
    // the window or has pressed the ESCAPE key.
    while (!glfwWindowShouldClose(window)) {
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

      glBufferSubData(GL_ARRAY_BUFFER, 0, loopback.getBuffer());

      glColor4f(0, 1, 0, 0);
      glBindBuffer(GL_ARRAY_BUFFER, vbo_vertex_handle);
      glVertexPointer(VERTEX_SIZE, GL_FLOAT, 0, 0L);
      glEnableClientState(GL_VERTEX_ARRAY);
      glDrawArrays(GL_POINTS, 0, NUM_VERTICES);
      glDisableClientState(GL_VERTEX_ARRAY);

      glfwSwapBuffers(window); // swap the color buffers

      // Poll for window events. The key callback above will only be
      // invoked during this call.
      glfwPollEvents();
    }
  }

  public static void main(String[] args) {
    new Sosci(new AudioLoopback(NUM_VERTICES)).run();
  }

}