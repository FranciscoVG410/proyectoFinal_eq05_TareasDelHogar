package valdez.francisco.dingdone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.FirebaseAuth

class HomeProfileFragment : Fragment() {

    private lateinit var tvHomeName: TextView
    private lateinit var etHomeNameEdit: EditText
    private lateinit var ivEditHomeName: ImageView
    private lateinit var tvHomeOwner: TextView
    private lateinit var tvMemberCount: TextView
    private lateinit var tvInvitationCode: TextView
    private lateinit var cbMembersCanEdit: CheckBox
    private lateinit var btnGoBack: Button

    private var home: Home? = null
    private var isUserOwner: Boolean = false
    private var isEditingName = false

    private val uvm: UserViewModel by activityViewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            home = it.getParcelable(ARG_HOME)
            isUserOwner = it.getBoolean(ARG_IS_OWNER, false)
        }

        if (home == null) {
            Toast.makeText(requireContext(), "Error: No se encontró la información del Home.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_profile, container, false)

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

        home?.let { h ->
            uvm.loadHomeDetails(h.id)

            setupPermissions(h, isUserOwner)
            setupNameEditListeners(h, isUserOwner)

            uvm.loadHomeMembers(h.id)
            uvm.loadCurrentHomeTasks(h.id)
        }

        setupObservers()
        setupHomeDetailsObserver()

        btnGoBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupObservers() {
        uvm.homeMembers.observe(viewLifecycleOwner) { members ->
            home?.let { h ->
                tvMemberCount.text = "Miembros: ${members.size}"

                val owner = members.find { it.id == h.ownerId }
                tvHomeOwner.text = "Owner: ${owner?.name ?: "Desconocido"}"
            }
        }
    }

    private fun setupHomeDetailsObserver() {
        uvm.currentHomeDetails.observe(viewLifecycleOwner) { updatedHome ->
            updatedHome?.let { h ->
                if (!isEditingName) {
                    tvHomeName.text = h.name
                    etHomeNameEdit.setText(h.name)
                }

                val canEditName = isUserOwner || h.membersCanEdit
                ivEditHomeName.visibility = if (canEditName) View.VISIBLE else View.GONE

                if (isUserOwner) {
                    cbMembersCanEdit.isChecked = h.membersCanEdit
                } else {
                    cbMembersCanEdit.visibility = View.GONE
                }

                tvInvitationCode.text = "Código de Invitación: ${h.invitationCode}"
            }
        }
    }

    private fun setupPermissions(home: Home, isOwner: Boolean) {

        if (isOwner) {
            cbMembersCanEdit.visibility = View.VISIBLE
            cbMembersCanEdit.text = "Permitir a todos los miembros editar Tareas y Nombre del Home"
            cbMembersCanEdit.isEnabled = true
            cbMembersCanEdit.isChecked = home.membersCanEdit

            cbMembersCanEdit.setOnCheckedChangeListener { _, isChecked ->
                uvm.updateHomeEditPermission(home.id, isChecked)
                val message = if (isChecked) "Permiso de edición activado." else "Permiso de edición desactivado."
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        } else {
            cbMembersCanEdit.visibility = View.GONE
        }
    }

    private fun setupNameEditListeners(h: Home, isOwner: Boolean) {

        ivEditHomeName.setOnClickListener {

            if (ivEditHomeName.visibility != View.VISIBLE) {
                Toast.makeText(requireContext(), "No tienes permiso para editar el nombre del Home.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEditingName) {
                val newName = etHomeNameEdit.text.toString().trim()

                if (newName.isBlank() || newName == tvHomeName.text.toString()) {
                    Toast.makeText(requireContext(), "El nombre no puede estar vacío y debe ser diferente.", Toast.LENGTH_SHORT).show()
                    enterViewMode(tvHomeName.text.toString())
                } else {
                    uvm.updateHomeName(h.id, newName)
                    Toast.makeText(requireContext(), "Nombre de Home actualizado.", Toast.LENGTH_SHORT).show()
                    enterViewMode(newName)
                }

            } else {
                enterEditMode()
            }
        }
    }

    private fun enterEditMode() {
        isEditingName = true
        tvHomeName.visibility = View.GONE
        etHomeNameEdit.visibility = View.VISIBLE
        etHomeNameEdit.requestFocus()
        ivEditHomeName.setImageResource(android.R.drawable.ic_menu_save)
    }

    private fun enterViewMode(finalName: String) {
        isEditingName = false
        tvHomeName.text = finalName
        tvHomeName.visibility = View.VISIBLE
        etHomeNameEdit.visibility = View.GONE
        ivEditHomeName.setImageResource(android.R.drawable.ic_menu_edit)
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