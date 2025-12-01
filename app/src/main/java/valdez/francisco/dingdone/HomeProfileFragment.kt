package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.FirebaseAuth

class HomeProfileFragment : Fragment() {

    // UI Components
    private lateinit var tvHomeName: TextView
    private lateinit var etHomeNameEdit: EditText
    private lateinit var ivEditHomeName: ImageView
    private lateinit var tvHomeOwner: TextView
    private lateinit var tvMemberCount: TextView
    private lateinit var tvInvitationCode: TextView
    private lateinit var cbMembersCanEdit: CheckBox
    private lateinit var btnGoBack: Button

    // Data Variables
    private var homeIdArg: String? = null
    private var isUserOwnerArg: Boolean = false

    // Estado local solo para la UI de edición
    private var isEditingName = false

    private val auth = FirebaseAuth.getInstance()
    private val uvm: UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recuperamos argumentos básicos.
        // Nota: Es más seguro pasar el ID y recargar datos frescos que pasar todo el objeto Home parcelable.
        arguments?.let {
            val homeParcel = it.getParcelable<Home>(ARG_HOME)
            homeIdArg = homeParcel?.id
            isUserOwnerArg = it.getBoolean(ARG_IS_OWNER, false)
        }

        if (homeIdArg == null) {
            Toast.makeText(requireContext(), "Error: No se encontró la información del Home.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_profile, container, false)

        // Inicialización de vistas
        tvHomeName = view.findViewById(R.id.tv_homeName)
        etHomeNameEdit = view.findViewById(R.id.et_homeNameEdit)
        ivEditHomeName = view.findViewById(R.id.iv_editHomeName)
        tvHomeOwner = view.findViewById(R.id.tv_homeOwner)
        tvMemberCount = view.findViewById(R.id.tv_memberCount)
        tvInvitationCode = view.findViewById(R.id.tv_invitationCode)
        cbMembersCanEdit = view.findViewById(R.id.cb_membersCanEdit)
        btnGoBack = view.findViewById(R.id.btn_goBack)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentHomeId = homeIdArg ?: return
        uvm.loadHomeProfileFullDetails(currentHomeId)
        setupObservers()
        setupListeners(currentHomeId)

    }

    private fun setupObservers() {

        uvm.currentHomeDetails.observe(viewLifecycleOwner) { home ->
            if (home != null) {

                updateHomeUi(home)

            }

        }

        uvm.homeOwnerName.observe(viewLifecycleOwner) { ownerName ->

            tvHomeOwner.text = "Owner: $ownerName"

        }

        uvm.homeMemberCount.observe(viewLifecycleOwner) { count ->

            tvMemberCount.text = "Miembros: $count"

        }

    }

    private fun updateHomeUi(home: Home) {

        if (!isEditingName) {

            tvHomeName.text = home.name
            etHomeNameEdit.setText(home.name)

        }

        tvInvitationCode.text = "Código de Invitación: ${home.invitationCode}"

        val canEdit = isUserOwnerArg
        ivEditHomeName.visibility = if (canEdit) View.VISIBLE else View.GONE

        if (isUserOwnerArg) {

            cbMembersCanEdit.visibility = View.VISIBLE
            cbMembersCanEdit.setOnCheckedChangeListener(null)
            cbMembersCanEdit.isChecked = home.membersCanEdit
            cbMembersCanEdit.setOnCheckedChangeListener { _, isChecked ->

                uvm.updateHomeEditPermission(home.id, isChecked)
                val msg = if (isChecked) "Todos pueden editar ahora" else "Solo el dueño puede editar"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

            }

        } else {

            cbMembersCanEdit.visibility = View.GONE

        }

    }

    private fun setupListeners(homeId: String) {

        btnGoBack.setOnClickListener {

            parentFragmentManager.popBackStack()

        }

        ivEditHomeName.setOnClickListener {

            handleEditNameClick(homeId)

        }

    }

    private fun handleEditNameClick(homeId: String) {

        if (isEditingName) {

            val newName = etHomeNameEdit.text.toString().trim()
            if (newName.isBlank()) {

                Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                return

            }

            if (newName == tvHomeName.text.toString()) {

                toggleEditMode(false)
                return

            }

            uvm.updateHomeName(homeId, newName)
            Toast.makeText(requireContext(), "Actualizando nombre...", Toast.LENGTH_SHORT).show()

            toggleEditMode(false)

        } else {

            toggleEditMode(true)

        }

    }

    private fun toggleEditMode(enable: Boolean) {

        isEditingName = enable
        if (enable) {

            tvHomeName.visibility = View.GONE
            etHomeNameEdit.visibility = View.VISIBLE
            etHomeNameEdit.requestFocus()
            ivEditHomeName.setImageResource(android.R.drawable.ic_menu_save)

        } else {

            tvHomeName.visibility = View.VISIBLE
            etHomeNameEdit.visibility = View.GONE
            ivEditHomeName.setImageResource(android.R.drawable.ic_menu_edit)

        }

    }

    companion object {

        private const val ARG_HOME = "home_object"
        private const val ARG_IS_OWNER = "is_owner_status"

        fun newInstance(home: Home, isOwner: Boolean): HomeProfileFragment {

            val fragment = HomeProfileFragment()
            val args = Bundle()
            args.putParcelable(ARG_HOME, home)
            args.putBoolean(ARG_IS_OWNER, isOwner)
            fragment.arguments = args
            return fragment

        }
    }
}
