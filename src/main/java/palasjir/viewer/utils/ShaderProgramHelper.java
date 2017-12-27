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
        add(gl, GL_VERTEX_SHADER, vertexShader);
        add(gl, GL_FRAGMENT_SHADER, fragmentShader);
    }

    public ShaderProgram program() {
        return this.program;
    }

    private String shaderName(String name) {
        return "shaders/" + name;
    }

    private ShaderCode create(GL4 gl, int type, String name) {
        return ShaderCode.create(
                gl,
                type,
                1,
                this.getClass(),
                new String[]{shaderName(name)},
                false
        );
    }

    private void add(GL4 gl, int type, String name) {
        ShaderCode code = create(gl, type, name);
        program.add(gl, code, System.err);
    }
}
