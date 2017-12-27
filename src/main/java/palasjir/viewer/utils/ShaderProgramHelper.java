package palasjir.viewer.utils;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import static com.jogamp.opengl.GL2ES2.*;

public class ShaderProgramHelper {

    private final String fragmentShader;
    private final String vertexShader;
    private ShaderProgram program;

    public ShaderProgramHelper(String vertexShader, String fragmentShader) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
    }

    public void init(GL4 gl) {
        program = new ShaderProgram();

        ShaderCode vp = ShaderCode.create(
                gl, GL_VERTEX_SHADER, 1, this.getClass(), new String[]{shaderName(vertexShader)}, false
        );
        ShaderCode fp = ShaderCode.create(
                gl, GL_FRAGMENT_SHADER, 1, this.getClass(), new String[]{shaderName(fragmentShader)}, false
        );
        program.add(gl, vp, System.err);
        program.add(gl, fp, System.err);
    }

    public ShaderProgram program() {
        return this.program;
    }

    private String shaderName(String name) {
        return "shaders/" + name;
    }
}
