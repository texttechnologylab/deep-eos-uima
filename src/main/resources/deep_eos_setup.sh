# abort at error
set -e

# params from java
CONDA_INSTALL_DIR="$1"
ENV_NAME="$2"

# activate "base" conda
echo "activating conda installation in $CONDA_INSTALL_DIR"
source "$CONDA_INSTALL_DIR/etc/profile.d/conda.sh"
conda activate

# activate env
conda activate "$ENV_NAME"
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:"$CONDA_INSTALL_DIR/$ENV_NAME/lib/"

# TODO: Download models from TTLab servers? 
